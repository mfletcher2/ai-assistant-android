package lol.max.assistantgpt.api.voice

import android.content.Context
import android.service.voice.VoiceInteractionSession
import android.view.View


class GPTInteractionSession(context: Context) : VoiceInteractionSession(context) {
    override fun onCreateContentView(): View? {
        finish()
        return null
    }
}