package lol.max.assistantgpt.api

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.theokanning.openai.assistants.AssistantToolsEnum
import com.theokanning.openai.assistants.Tool
import com.theokanning.openai.completion.chat.ChatFunction
import java.lang.ref.WeakReference
import java.time.DayOfWeek
import java.util.Calendar

class FunctionCalling(context: Context?) {
    private val contextRef = WeakReference(context)

    private val getAppsListFunction: ChatFunction = ChatFunction.builder()
        .name("get_apps_list")
        .description("Get a list of installed applications.")
        .executor(PackageListRequest::class.java) { it.getPackages(contextRef.get()) }
        .build()
    private val launchPackageFunction: ChatFunction = ChatFunction.builder()
        .name("launch_package")
        .description("Launches the given package. Always confirm the package name first with get_apps_list(). Returns whether or not it was successful.")
        .executor(PackageRunRequest::class.java) { it.runPackage(contextRef.get()) }
        .build()
    private val getDateAndTimeFunction: ChatFunction = ChatFunction.builder()
        .name("get_date_and_time")
        .description("Get the current date and time in the user's local time zone.")
        .executor(DateAndTimeRequest::class.java) { it.getDateAndTime() }
        .build()

    val functionList =
        listOf(
            cseChatFunction,
            getAppsListFunction,
            launchPackageFunction,
            weatherChatFunction,
            getDateAndTimeFunction
        )
    private val functionToolsList: List<Tool> = List(functionList.size) {
        Tool(
            AssistantToolsEnum.FUNCTION,
            functionList[it]
        )
    }

    fun getFunctionToolsList(allowSensors: Boolean = true): List<Tool> {
        return if(allowSensors)
            functionToolsList
        else listOf(functionToolsList[0], functionToolsList[3], functionToolsList[4])
    }

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
            val calendar = Calendar.getInstance()
            return DateAndTime(
                DayOfWeek.of(calendar.get(Calendar.DAY_OF_WEEK)).name,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
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
}