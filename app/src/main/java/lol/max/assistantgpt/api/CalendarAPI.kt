package lol.max.assistantgpt.api

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import java.util.*

class CalendarGetEventsRequest : LateResponse() {
    @JsonPropertyDescription("The number of days to get events for. Defaults to 7.")
    var numDays: Int = 7

    fun getEvents(context: Context?) {
        if (context == null) {
            onSuccess("An error occurred")
            return
        }

        val INSTANCE_PROJECTION: Array<String> = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances._ID
        )
        // The indices for the projection array above.
        val PROJECTION_TITLE_INDEX = 0
        val PROJECTION_BEGIN_INDEX = 1
        val PROJECTION_ID_INDEX = 2

        val beginTime = System.currentTimeMillis()
        val endTime = beginTime + numDays * 24 * 60 * 60 * 1000

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, beginTime)
        ContentUris.appendId(uriBuilder, endTime)

        // Run query
        val cur = context.contentResolver.query(
            uriBuilder.build(),
            INSTANCE_PROJECTION,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC"
        )

        if (cur == null) {
            onSuccess("No events were found")
            return
        }

        val result = arrayListOf<Map<String, String>>()
        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            // Get the field values
//        val calID: Long = cur.getLong(PROJECTION_ID_INDEX)
            val eventTitle = cur.getString(PROJECTION_TITLE_INDEX) ?: ""
            val eventStart = cur.getLong(PROJECTION_BEGIN_INDEX)

            val eventDate = Date(eventStart)

            result += mapOf("title" to eventTitle, "date" to eventDate.toString())
        }
        cur.close()

        onSuccess(result)
    }
}

class CalendarCreateEventRequest {
    @JsonProperty(required = true)
    var startYear: Int = 0

    @JsonProperty(required = true)
    var startMonth: Int = 0

    @JsonProperty(required = true)
    var startDay: Int = 0

    @JsonProperty(required = true)
    var startHour: Int = 0

    @JsonProperty(required = true)
    var startMinute: Int = 0

    @JsonProperty(required = true)
    var endYear: Int = 0

    @JsonProperty(required = true)
    var endMonth: Int = 0

    @JsonProperty(required = true)
    var endDay: Int = 0

    @JsonProperty(required = true)
    var endHour: Int = 0

    @JsonProperty(required = true)
    var endMinute: Int = 0

    @JsonProperty(required = true)
    lateinit var title: String
    var description: String? = null
    var location: String? = null

    fun createEvent(context: Context?): Boolean {
        if (context == null)
            return false

        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(
                CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                Calendar.getInstance()
                    .apply { set(startYear, startMonth, startDay, startHour, startMinute) }.timeInMillis
            )
            .putExtra(
                CalendarContract.EXTRA_EVENT_END_TIME,
                Calendar.getInstance().apply { set(endYear, endMonth, endDay, endHour, endMinute) }.timeInMillis
            )
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.Events.DESCRIPTION, description)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        context.startActivity(intent)

        return true
    }
}