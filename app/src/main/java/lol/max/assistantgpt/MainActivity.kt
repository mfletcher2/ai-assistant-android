package lol.max.assistantgpt

import android.os.Build
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import lol.max.assistantgpt.ui.ChatScreen
import lol.max.assistantgpt.ui.theme.AssistantGPTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tts = TextToSpeech(
            this
        ) { status ->
            if (status == TextToSpeech.SUCCESS)
                Log.i(getString(R.string.app_name), "TTS initialized")
            else Log.e(getString(R.string.app_name), "TTS failed to initialize")
        }
        val stt =
            if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this))
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            else SpeechRecognizer.createSpeechRecognizer(this)

        // Update UI elements
        setContent {
            AssistantGPTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(tts, stt)
                }
            }
        }
    }

}
