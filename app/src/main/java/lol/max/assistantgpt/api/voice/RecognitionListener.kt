package lol.max.assistantgpt.api.voice

import android.os.Bundle
import android.speech.SpeechRecognizer
import android.util.Log

class RecognitionListener(
    private val updateText: (String) -> Unit,
    private val runOnEndOfSpeech: (statusCode: Int) -> Unit
) :
    android.speech.RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) {
        Log.i("SpeechRecognizer", "Speech recognition ready")
        updateText("")
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        Log.e("SpeechRecognizer", "Error code $error")
        runOnEndOfSpeech(error)
    }

    override fun onResults(results: Bundle?) {
        if (results == null) {
            Log.e("SpeechRecognizer", "No results found")
            return
        } else {
            val arr = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
            updateText(arr[0])
        }
        runOnEndOfSpeech(0)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        if (partialResults == null) return
        val arr = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        updateText(arr[0])
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}