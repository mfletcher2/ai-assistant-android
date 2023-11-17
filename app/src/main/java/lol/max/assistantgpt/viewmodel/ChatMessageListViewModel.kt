package lol.max.assistantgpt.viewmodel

import androidx.lifecycle.ViewModel
import com.theokanning.openai.completion.chat.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ChatMessageListUiState(
    val chatList: List<ChatMessage> = arrayListOf()
)

class ChatMessageListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatMessageListUiState())
    val uiState: StateFlow<ChatMessageListUiState> = _uiState.asStateFlow()

    fun addMessage(message: ChatMessage) {
        _uiState.update { currentState ->
            currentState.copy(
                chatList = currentState.chatList + message
            )
        }
    }

    fun addMessages(messages: List<ChatMessage>) {
        _uiState.update { currentState ->
            currentState.copy(
                chatList = currentState.chatList + messages
            )
        }
    }

    fun getMessages(): List<ChatMessage> {
        return _uiState.value.chatList
    }
}