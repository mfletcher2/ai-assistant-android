package lol.max.assistantgpt.api

import android.Manifest
import android.content.Context
import com.fasterxml.jackson.annotation.JsonIgnore
import com.theokanning.openai.completion.chat.ChatFunction
import java.lang.ref.WeakReference

class ChatFunctions(context: Context, sensorValues: SensorValues) {
    private val contextRef = WeakReference(context)

    private val appsListChatFunction = ChatFunction.builder()
        .name("get_apps_list")
        .description("Get a list of installed applications.")
        .executor(PackageListRequest::class.java) { it.getPackages(contextRef.get()) }
        .build()
    private val launchPackageChatFunction = ChatFunction.builder()
        .name("launch_package")
        .description("Launches the given package. Always confirm the package name first with get_apps_list(). Returns whether the launch was successful.")
        .executor(PackageRunRequest::class.java) { it.runPackage(contextRef.get()) }
        .build()
    private val dateAndTimeChatFunction = ChatFunction.builder()
        .name("get_date_and_time")
        .description("Get the current date and time in the user's local time zone.")
        .executor(DateAndTimeRequest::class.java) { it.getDateAndTime() }
        .build()
    private val weatherChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_weather")
        .description("Get the weather forecast for a location in the United States.")
        .executor(WeatherAPI::class.java) { it.getWeather(it.address) }
        .build()
    private val weatherByLocationChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_weather_current_location")
        .description("Get the weather forecast for the user's current location.")
        .executor(WeatherByLatLonRequest::class.java) { it.getWeatherFromLocation(contextRef.get()) }
        .build()
    private val calendarGetEventsChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_calendar_events")
        .description("Get the user's calendar events.")
        .executor(CalendarGetEventsRequest::class.java) { it.getEvents(contextRef.get()) }
        .build()
    private val calendarCreateEventChatFunction: ChatFunction = ChatFunction.builder()
        .name("create_calendar_event")
        .description("Create a calendar event. Returns true if the event was created successfully. Ask the user if all required information is not given. If given a relative date, always confirm the current date with get_date_and_time.")
        .executor(CalendarCreateEventRequest::class.java) { it.createEvent(contextRef.get()) }
        .build()
    private val createTimerChatFunction: ChatFunction = ChatFunction.builder()
        .name("create_timer")
        .description("Create a timer. Returns true if the timer was created successfully.")
        .executor(SetTimerRequest::class.java) { it.setTimer(contextRef.get()) }
        .build()
    private val createAlarmChatFunction: ChatFunction = ChatFunction.builder()
        .name("create_alarm")
        .description("Create an alarm. Returns true if the alarm was created successfully.")
        .executor(SetAlarmRequest::class.java) { it.setAlarm(contextRef.get()) }
        .build()

    // functions that access sensors
    private val accelerometerChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_accelerometer")
        .description("Get the current device's accelerometer x, y, and z values in meters per second squared. The accelerometer takes into account acceleration due to gravity.")
        .executor(LateResponse::class.java) { it.onSuccess(sensorValues.accelerometer) }
        .build()
    private val lightChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_light")
        .description("Get the current device's light sensor value in lux.")
        .executor(LateResponse::class.java) { it.onSuccess(sensorValues.light) }
        .build()
    private val orientationChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_orientation")
        .description("Get the current device's orientation azimuth, pitch, and roll values in radians.")
        .executor(SensorFunctions.OrientationRequest::class.java) { it.getOrientation(sensorValues) }
        .build()
    private val pressureChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_pressure")
        .description("Get the current device's pressure sensor value in hectopascals.")
        .executor(LateResponse::class.java) { it.onSuccess(sensorValues.pressure) }
        .build()
    private val stepCounterChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_step_counter")
        .description("Get the current device's step counter value. This value counts the number of steps taken since the device was last rebooted.")
        .executor(LateResponse::class.java) { it.onSuccess(sensorValues.stepCounter) }
        .build()

    fun getFunctionList(allowSensors: Boolean = true): List<ChatFunction> {
        return listOf(
            cseChatFunction,
            weatherChatFunction,
            dateAndTimeChatFunction,
            nytTopStoriesFunction,
            nytArticleSearchFunction,
            calendarGetEventsChatFunction,
            calendarCreateEventChatFunction,
            createTimerChatFunction,
            createAlarmChatFunction
        ).apply {
            if (allowSensors) plus( // Add sensor functions if allowed
                listOf(
                    appsListChatFunction,
                    launchPackageChatFunction,
                    accelerometerChatFunction,
                    lightChatFunction,
                    orientationChatFunction,
                    pressureChatFunction,
                    stepCounterChatFunction
                )
            )
        }
    }

    val requiresPermission: Map<ChatFunction, Pair<String?, String>> =
        mapOf(
            weatherByLocationChatFunction to Pair(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                "location"
            ),
            accelerometerChatFunction to Pair(null, "accelerometer"),
            lightChatFunction to Pair(null, "light sensor"),
            orientationChatFunction to Pair(null, "orientation"),
            pressureChatFunction to Pair(null, "pressure sensor"),
            stepCounterChatFunction to Pair(
                Manifest.permission.ACTIVITY_RECOGNITION,
                "step counter"
            ),
            calendarGetEventsChatFunction to Pair(Manifest.permission.READ_CALENDAR, "calendar")
        )

}

open class LateResponse {
    @JsonIgnore
    var onSuccess: (result: Any) -> Unit = {}
}
