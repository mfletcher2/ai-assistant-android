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
    val calendarGetEventsChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_calendar_events")
        .description("Get the user's calendar events.")
        .executor(CalendarGetEventsRequest::class.java) { it.getEvents(contextRef.get()) }
        .build()
    val calendarCreateEventChatFunction: ChatFunction = ChatFunction.builder()
        .name("create_calendar_event")
        .description("Create a calendar event. Returns true if the event was created successfully. Ask the user if all required information is not given. If given a relative date, always confirm the current date with get_date_and_time.")
        .executor(CalendarCreateEventRequest::class.java) { it.createEvent(contextRef.get()) }
        .build()


    // functions that access sensors
    val accelerometerChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_accelerometer")
        .description("Get the current device's accelerometer x, y, and z values in meters per second squared. The accelerometer takes into account acceleration due to gravity.")
        .executor(LateResponse::class.java) { it.onSuccess(sensorValues.accelerometer) }
        .build()
    val lightChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_light")
        .description("Get the current device's light sensor value in lux.")
        .executor(LateResponse::class.java) { it.onSuccess(sensorValues.light) }
        .build()
    val orientationChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_orientation")
        .description("Get the current device's orientation azimuth, pitch, and roll values in radians.")
        .executor(SensorFunctions.OrientationRequest::class.java) { it.getOrientation(sensorValues) }
        .build()
    val pressureChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_pressure")
        .description("Get the current device's pressure sensor value in hectopascals.")
        .executor(LateResponse::class.java) { it.onSuccess(sensorValues.pressure) }
        .build()
    val stepCounterChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_step_counter")
        .description("Get the current device's step counter value. This value counts the number of steps taken since the device was last rebooted.")
        .executor(LateResponse::class.java) { it.onSuccess(sensorValues.stepCounter) }
        .build()

    fun getFunctionList(allowSensors: Boolean = true): List<ChatFunction> {
        return if (allowSensors) listOf(
            cseChatFunction,
            appsListChatFunction,
            launchPackageChatFunction,
            weatherChatFunction,
            weatherByLocationChatFunction,
            dateAndTimeChatFunction,
            nytTopStoriesFunction,
            nytArticleSearchFunction,
            calendarGetEventsChatFunction,
            calendarCreateEventChatFunction,
            accelerometerChatFunction,
            lightChatFunction,
            orientationChatFunction,
            pressureChatFunction,
            stepCounterChatFunction
        )
        else listOf(
            cseChatFunction,
            weatherChatFunction,
            dateAndTimeChatFunction,
            nytTopStoriesFunction,
            nytArticleSearchFunction,
            calendarGetEventsChatFunction
        )
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
