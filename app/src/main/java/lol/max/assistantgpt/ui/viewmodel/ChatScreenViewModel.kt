package lol.max.assistantgpt.ui.viewmodel

import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lol.max.assistantgpt.api.ChatAPI
import lol.max.assistantgpt.ui.DialogTypes
import lol.max.assistantgpt.ui.Options

data class ChatScreenUiState(
    val chatList: List<ChatMessage> = arrayListOf(),
    val options: Options = Options(""),
    val enableButtons: Boolean = true,
    val enableWaitingIndicator: Boolean = false,
)

class ChatScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatScreenUiState())
    val uiState: StateFlow<ChatScreenUiState> = _uiState.asStateFlow()

    var chatInput by mutableStateOf("")
        private set

    fun updateChatInput(str: String) {
        chatInput = str
    }

    fun sendChatInput(
        coroutineScope: CoroutineScope,
        chatApi: ChatAPI,
        options: Options,
        snackbarHostState: SnackbarHostState,
        onSuccess: (String) -> Unit
    ) {
        if (chatInput == "") return
        _uiState.update {
            it.copy(
                enableButtons = false,
                enableWaitingIndicator = true,
                chatList = it.chatList + ChatMessage(ChatMessageRole.USER.value(), chatInput)
            )
        }

        chatInput = ""

        coroutineScope.launch(Dispatchers.IO) {
            val newMessages =
                chatApi.getCompletion(
                    _uiState.value.chatList,
                    options.model,
                    snackbarHostState
                )

            _uiState.update {
                it.copy(
                    enableButtons = true,
                    enableWaitingIndicator = false,
                    chatList = it.chatList + newMessages
                )
            }
            onSuccess(newMessages[newMessages.size - 1].content)
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
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        stt.startListening(i)
    }

    fun endVoiceChatInput(
        statusCode: Int, coroutineScope: CoroutineScope,
        chatApi: ChatAPI,
        options: Options,
        snackbarHostState: SnackbarHostState,
        onSuccess: (String) -> Unit
    ) {
        if (statusCode == 0 && chatInput != "") {
            sendChatInput(
                coroutineScope,
                chatApi,
                options,
                snackbarHostState
            ) { onSuccess(_uiState.value.chatList[_uiState.value.chatList.size - 1].content) }
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
}
