package lol.max.assistantgpt.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lol.max.assistantgpt.BuildConfig
import lol.max.assistantgpt.R
import lol.max.assistantgpt.api.ChatAPI
import lol.max.assistantgpt.api.GPTModel
import lol.max.assistantgpt.api.SensorValues
import lol.max.assistantgpt.api.availableModels
import lol.max.assistantgpt.ui.dialog.DialogTypes
import lol.max.assistantgpt.ui.dialog.SensorRequest

data class ChatScreenUiState(
    val chatList: ArrayList<ChatMessage> = arrayListOf(),
    val enableButtons: Boolean = true,
    val enableWaitingIndicator: Boolean = false,
    val newMessageAnimated: MutableState<Boolean> = mutableStateOf(true)
)

class ChatScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChatScreenUiState())
    val uiState: StateFlow<ChatScreenUiState> = _uiState.asStateFlow()

    private val sharedPreferences = application.applicationContext.getSharedPreferences(
        application.getString(R.string.app_name),
        Context.MODE_PRIVATE
    )
    var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    val options = Options(
        model = Gson().fromJson(
            sharedPreferences.getString(
                "model",
                Gson().toJson(Options.Default.model)
            )!!, GPTModel::class.java
        ),
        timeoutSec = sharedPreferences.getInt("timeoutSec", Options.Default.timeoutSec),
        openAiKey = sharedPreferences.getString("openAiKey", Options.Default.openAiKey)!!,
        googleKey = sharedPreferences.getString("googleKey", Options.Default.googleKey)!!,
        googleSearchId = sharedPreferences.getString(
            "googleSearchId",
            Options.Default.googleSearchId
        )!!,
        allowSensors = sharedPreferences.getBoolean("allowSensors", Options.Default.allowSensors),
        confirmSensors = sharedPreferences.getBoolean(
            "confirmSensors",
            Options.Default.confirmSensors
        )
    )

    private val chatApi = ChatAPI(BuildConfig.OPENAI_API_KEY, options.timeoutSec)

    var chatInput by mutableStateOf("")
        private set

    fun updateChatInput(str: String) {
        chatInput = str
    }

    val sensorRequest = SensorRequest(
        sensorName = "",
        permission = "",
        onGranted = { },
        onDenied = { },
        onClose = { updateShowDialog(DialogTypes.NONE) },
        showDialog = { updateShowDialog(DialogTypes.SENSOR) })

    fun sendChatInput(
        activity: ComponentActivity,
        snackbarHostState: SnackbarHostState,
        coroutineScope: CoroutineScope,
        sensorValues: SensorValues,
        onSuccess: (String) -> Unit
    ) {
        if (chatInput == "") return
        val newChatList = _uiState.value.chatList
        newChatList.add(ChatMessage(ChatMessageRole.USER.value(), chatInput.trim()))
        _uiState.update {
            it.copy(
                enableButtons = false,
                enableWaitingIndicator = true,
                chatList = newChatList
            )
        }

        chatInput = ""

        coroutineScope.launch(Dispatchers.IO) {
            chatApi.getCompletion(
                chatMessages = _uiState.value.chatList,
                model = options.model,
                context = activity,
                allowSensors = options.allowSensors,
                permissionRequestLauncher = requestPermissionLauncher!!,
                sensorRequest = sensorRequest,
                sensorValues = sensorValues,
                showMessage = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                    }
                }, updateChatMessageList = { list ->
                    _uiState.update {
                        it.copy(
                            enableButtons = true,
                            enableWaitingIndicator = false,
                            chatList = list,
                            newMessageAnimated = mutableStateOf(false)
                        )
                    }
                    if (list.isNotEmpty() && list.last().content != null)
                        onSuccess(list.last().content)
                })
        }
    }

    fun startChatVoiceInput(stt: SpeechRecognizer) {
        _uiState.update {
            it.copy(
                enableButtons = false
            )
        }
        updateShowDialog(DialogTypes.VOICE)
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            i.putExtra(
                RecognizerIntent.EXTRA_ENABLE_FORMATTING,
                RecognizerIntent.FORMATTING_OPTIMIZE_LATENCY
            )
            i.putExtra(RecognizerIntent.EXTRA_MASK_OFFENSIVE_WORDS, false)
        }
        i.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        stt.startListening(i)
    }

    fun endVoiceChatInput(
        activity: ComponentActivity,
        statusCode: Int, coroutineScope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        sensorValues: SensorValues,
        onSuccess: (String) -> Unit
    ) {
        updateShowDialog(DialogTypes.NONE)
        if (statusCode == 0 && chatInput != "") {
            sendChatInput(
                activity,
                snackbarHostState,
                coroutineScope,
                sensorValues
            ) { onSuccess(_uiState.value.chatList[_uiState.value.chatList.size - 1].content) }
        } else _uiState.update {
            it.copy(
                enableButtons = true
            )
        }
    }

    fun setMessagesList(list: ArrayList<ChatMessage>) {
        _uiState.update {
            it.copy(
                chatList = list
            )
        }
    }

    var showDialog: DialogTypes by mutableStateOf(DialogTypes.NONE)
        private set

    fun updateShowDialog(newDialog: DialogTypes) {
        showDialog = newDialog
    }

    fun saveSharedPreferences() {
        val e = sharedPreferences.edit()
        e.putString("model", Gson().toJson(options.model))
        e.putInt("timeoutSec", options.timeoutSec)
        e.putString("openAiKey", options.openAiKey)
        e.putString("googleKey", options.googleKey)
        e.putString("googleSearchId", options.googleSearchId)
        e.putBoolean("allowSensors", options.allowSensors)
        e.putBoolean("confirmSensors", options.confirmSensors)
        e.apply()
    }

    fun updateTimeoutSec() {
        chatApi.setTimeoutSec(options.timeoutSec)
    }
}

class Options(
    var model: GPTModel,
    var timeoutSec: Int,
    var openAiKey: String,
    var googleKey: String,
    var googleSearchId: String,
    var allowSensors: Boolean,
    var confirmSensors: Boolean
) {

    companion object {
        val Default = Options(
            model = availableModels[0],
            timeoutSec = 60,
            openAiKey = "",
            googleKey = "",
            googleSearchId = "",
            allowSensors = false,
            confirmSensors = true
        )
    }
}

