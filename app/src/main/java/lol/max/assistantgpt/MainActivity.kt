package lol.max.assistantgpt

import android.content.SharedPreferences
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
import lol.max.assistantgpt.api.ChatAPI
import lol.max.assistantgpt.ui.ChatScreen
import lol.max.assistantgpt.ui.Options
import lol.max.assistantgpt.ui.theme.AssistantGPTTheme
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var options: Options

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tts = TextToSpeech(
            this
        ) { status ->
            if (status == TextToSpeech.SUCCESS)
                Log.i(getString(R.string.app_name), "TTS initialized")
            else Log.e(getString(R.string.app_name), "TTS failed to initialize")
        }
        val stt = SpeechRecognizer.createSpeechRecognizer(this)

        prefs = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        options = Options(prefs.getString("model", "gpt-3.5-turbo")!!)
        val chatApi = ChatAPI(BuildConfig.OPENAI_API_KEY, context = WeakReference(this))

        // Update UI elements
        setContent {
            AssistantGPTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(tts, stt, chatApi, options)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val e = prefs.edit()
        e.putString("model", options.model)
        e.apply()
    }

}


