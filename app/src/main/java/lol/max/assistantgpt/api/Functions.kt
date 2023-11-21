package lol.max.assistantgpt.api

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.theokanning.openai.completion.chat.ChatFunction
import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.LocalTime

class Functions(context: Context) {
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
    private val weatherByLocationChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_weather_current_location")
        .description("Get the weather forecast for the user's current location.")
        .executor(WeatherByLatLonAPI::class.java) { it.getWeatherFromLocation(contextRef.get()) }
        .build()

    fun getFunctionList(allowSensors: Boolean = true): List<ChatFunction> {
        return if (allowSensors) listOf(
            cseChatFunction,
            appsListChatFunction,
            launchPackageChatFunction,
            weatherChatFunction,
            weatherByLocationChatFunction,
            dateAndTimeChatFunction,
        )
        else listOf(cseChatFunction, weatherChatFunction, dateAndTimeChatFunction)
    }

    val requiresPermission: Map<ChatFunction, Pair<String, String>> =
        mapOf(
            weatherByLocationChatFunction to Pair(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                "location"
            )
        )

    class PackageListRequest {
        fun getPackages(context: Context?): List<PackageResult> {
            if (context == null)
                return listOf()

            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            val pkgAppsList: List<ResolveInfo> =
                context.packageManager.queryIntentActivities(mainIntent, 0)

            val resultList = ArrayList<PackageResult>()
            pkgAppsList.forEach {
                resultList.add(
                    PackageResult(
                        it.loadLabel(context.packageManager).toString(),
                        it.activityInfo.packageName
                    )
                )
            }
            return resultList
        }
    }

    data class PackageResult(val appName: String, val packageName: String)

    class PackageRunRequest {
        @JsonPropertyDescription("Name of the package to run")
        @JsonProperty(required = true)
        lateinit var packageName: String
        fun runPackage(context: Context?): Boolean {
            if (context == null) return false
            val i = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
            i.addCategory(Intent.CATEGORY_LAUNCHER)
            context.startActivity(i)
            return true
        }
    }

    class DateAndTimeRequest {
        fun getDateAndTime(): DateAndTime {
            val date = LocalDate.now()
            val time = LocalTime.now()
            return DateAndTime(
                date.dayOfWeek.name,
                date.dayOfMonth,
                date.monthValue,
                date.year,
                time.hour,
                time.minute,
                time.second
            )
        }
    }

    data class DateAndTime(
        val dayOfWeek: String,
        val dayOfMonth: Int,
        val month: Int,
        val year: Int,
        val hour: Int,
        val minute: Int,
        val second: Int
    )

    abstract class LateResponse {
        var onSuccess: (result: Any) -> Unit = {}
    }
}