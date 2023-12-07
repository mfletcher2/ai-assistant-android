package lol.max.assistantgpt.api.voice

import android.content.Intent
import android.speech.RecognitionService
import android.util.Log

class GPTRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        Log.i("SpeechRecognizer", "Listening started")
    }

    override fun onCancel(listener: Callback?) {
    }

    override fun onStopListening(listener: Callback?) {
    }
}