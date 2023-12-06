package lol.max.assistantgpt.api

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

class SetTimerRequest {
    @JsonPropertyDescription("Time in seconds")
    @JsonProperty(required = true)
    var timeSeconds: Int = 0

    @JsonPropertyDescription("A custom message to identify the timer")
    var message: String = ""

    fun setTimer(context: Context?): Boolean {
        if (context == null) return false
        val i = Intent(AlarmClock.ACTION_SET_TIMER)
        i.putExtra(AlarmClock.EXTRA_LENGTH, timeSeconds)
        if (message != "") i.putExtra(AlarmClock.EXTRA_MESSAGE, message)

        try {
            context.startActivity(i)
        } catch (e: Exception) {
            return false
        }
        return true
    }
}

class SetAlarmRequest {
    @JsonPropertyDescription("Hour of the alarm")
    @JsonProperty(required = true)
    var hour: Int = 0

    @JsonPropertyDescription("Minute of the alarm")
    @JsonProperty(required = true)
    var minute: Int = 0

    @JsonPropertyDescription("A custom message to identify the alarm")
    var message: String = ""

    fun setAlarm(context: Context?): Boolean {
        if (context == null) return false
        val i = Intent(AlarmClock.ACTION_SET_ALARM)
        i.putExtra(AlarmClock.EXTRA_HOUR, hour)
        i.putExtra(AlarmClock.EXTRA_MINUTES, minute)
        if (message != "") i.putExtra(AlarmClock.EXTRA_MESSAGE, message)

        try {
            context.startActivity(i)
        } catch (e: Exception) {
            return false
        }
        return true
    }
}