package lol.max.assistantgpt

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import lol.max.assistantgpt.api.SensorFunctions
import lol.max.assistantgpt.api.chat.FunctionExecutor
import lol.max.assistantgpt.ui.ChatScreen
import lol.max.assistantgpt.ui.theme.AssistantGPTTheme

class MainActivity : ComponentActivity() {
    private lateinit var sensorFunctions: SensorFunctions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorFunctions = SensorFunctions(this)

        val startVoiceInput = intent?.getBooleanExtra("voice", false) ?: false

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    Log.i(getString(R.string.app_name), "Permission granted")
                    FunctionExecutor.onGranted()
                } else {
                    Log.e(getString(R.string.app_name), "Permission denied")
                    FunctionExecutor.onDenied()
                }
            }

        // Update UI elements
        setContent {
            AssistantGPTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(
                        sensorValues = sensorFunctions.sensorValues,
                        startVoiceInput = startVoiceInput,
                        requestPermission = { requestPermissionLauncher.launch(it) })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorFunctions.registerListeners()
    }

    override fun onPause() {
        super.onPause()
        sensorFunctions.unregisterListeners()
    }

}
