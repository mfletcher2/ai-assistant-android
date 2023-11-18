package lol.max.assistantgpt.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.messages.Message
import com.theokanning.openai.messages.MessageContent
import com.theokanning.openai.messages.content.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lol.max.assistantgpt.BuildConfig
import lol.max.assistantgpt.R
import lol.max.assistantgpt.api.AssistantsAPI
import lol.max.assistantgpt.ui.DialogTypes
import lol.max.assistantgpt.ui.availableModels
import kotlin.concurrent.thread

data class ChatScreenUiState(
    val chatList: List<Message> = arrayListOf(),
    val enableButtons: Boolean = false,
    val enableWaitingIndicator: Boolean = true,
)

class ChatScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChatScreenUiState())
    val uiState: StateFlow<ChatScreenUiState> = _uiState.asStateFlow()

    private var assistantsApi: AssistantsAPI

    private val sharedPreferences = application.applicationContext.getSharedPreferences(
        application.getString(R.string.app_name), Context.MODE_PRIVATE
    )
    val options = Options(
        model = sharedPreferences.getString("model", Options.Default.model)!!,
        timeoutSec = sharedPreferences.getInt("timeoutSec", Options.Default.timeoutSec),
        openAiKey = sharedPreferences.getString("openAiKey", Options.Default.openAiKey)!!,
        googleKey = sharedPreferences.getString("googleKey", Options.Default.googleKey)!!,
        googleSearchId = sharedPreferences.getString(
            "googleSearchId", Options.Default.googleSearchId
        )!!,
        allowSensors = sharedPreferences.getBoolean("allowSensors", Options.Default.allowSensors)
    )

    init {
        val gson = Gson()
        val type = object : TypeToken<List<String>>() {}.type

        assistantsApi = AssistantsAPI(
            application,
            if (options.openAiKey == "") BuildConfig.OPENAI_API_KEY else options.openAiKey,
            gson.fromJson(sharedPreferences.getString("threadIds", "[]"), type),
            sharedPreferences.getInt("currentThread", 0)
        ) { saveThreads() }
        thread {
            setThread(assistantsApi.currentThread)
        }

    }

    var chatInput by mutableStateOf("")
        private set

    fun updateChatInput(str: String) {
        chatInput = str
    }

    fun sendChatInput(
        snackbarHostState: SnackbarHostState,
        coroutineScope: CoroutineScope,
        onSuccess: (String) -> Unit
    ) {
        if (chatInput == "") return

        chatInput = chatInput.trim()

        val userMessage = Message()
        userMessage.role = ChatMessageRole.USER.value()
        userMessage.content = arrayListOf(MessageContent())
        userMessage.content[0].text = Text()
        userMessage.content[0].text.value = chatInput
        _uiState.update {
            it.copy(
                enableButtons = false,
                enableWaitingIndicator = true,
                chatList = listOf(userMessage) + it.chatList
            )
        }

        val tempInput = chatInput
        chatInput = ""

        coroutineScope.launch(Dispatchers.IO) {
            val newMessageList = assistantsApi.getResponse(
                tempInput, options.model, options.allowSensors
            ) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(it)
                }
            }

            _uiState.update {
                it.copy(
                    enableButtons = true,
                    enableWaitingIndicator = false,
                    chatList = newMessageList ?: _uiState.value.chatList
                )
            }
            if (newMessageList != null) onSuccess(newMessageList[0].content[0].text.value)
        }
    }

    fun startChatVoiceInput(stt: SpeechRecognizer) {
        _uiState.update {
            it.copy(
                enableButtons = false
            )
        }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            i.putExtra(
                RecognizerIntent.EXTRA_ENABLE_FORMATTING,
                RecognizerIntent.FORMATTING_OPTIMIZE_LATENCY
            )
            i.putExtra(RecognizerIntent.EXTRA_MASK_OFFENSIVE_WORDS, false)
        }
        i.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        stt.startListening(i)
    }

    fun endVoiceChatInput(
        statusCode: Int,
        coroutineScope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        onSuccess: (String) -> Unit
    ) {
        if (statusCode == 0 && chatInput != "") {
            sendChatInput(
                snackbarHostState, coroutineScope
            ) { onSuccess(_uiState.value.chatList[0].content[0].text.value) }
        } else _uiState.update {
            it.copy(
                enableButtons = true
            )
        }
    }

    var showDialog: DialogTypes by mutableStateOf(DialogTypes.NONE)
        private set

    fun updateShowDialog(newDialog: DialogTypes) {
        showDialog = newDialog
    }

    fun saveOptions() {
        val e = sharedPreferences.edit()
        e.putString("model", options.model)
        e.putInt("timeoutSec", options.timeoutSec)
        e.putString("openAiKey", options.openAiKey)
        e.putString("googleKey", options.googleKey)
        e.putString("googleSearchId", options.googleSearchId)
        e.apply()
    }

    private fun saveThreads() {
        val e = sharedPreferences.edit()
        val gson = Gson()
        e.putString("threadIds", gson.toJson(assistantsApi.threadIds))
        e.putInt("currentThread", assistantsApi.currentThread)
        e.apply()
    }

    private fun setThread(threadIdx: Int) {
        assistantsApi.currentThread = threadIdx

        _uiState.update {
            it.copy(
                chatList = assistantsApi.getMessagesFromAPI() ?: _uiState.value.chatList,
                enableButtons = true,
                enableWaitingIndicator = false
            )
        }
    }
}


class Options(
    var model: String,
    var timeoutSec: Int,
    var openAiKey: String,
    var googleKey: String,
    var googleSearchId: String,
    var allowSensors: Boolean
) {

    companion object {
        val Default = Options(
            model = availableModels[0],
            timeoutSec = 60,
            openAiKey = "",
            googleKey = "",
            googleSearchId = "",
            allowSensors = true
        )
    }
}

