package lol.max.assistantgpt.api.voice

import android.content.Context
import android.content.Intent
import android.service.voice.VoiceInteractionSession
import lol.max.assistantgpt.MainActivity


class GPTInteractionSession(context: Context) : VoiceInteractionSession(context) {
    override fun onCreate() {
        super.onCreate()
        val i = Intent(context, MainActivity::class.java)
        i.putExtra("voice", true)
        startAssistantActivity(i)
        finish()
    }
}