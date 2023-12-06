package lol.max.assistantgpt.api

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import java.time.LocalDate
import java.time.LocalTime

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

