package lol.max.assistantgpt.ui.viewmodel

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import lol.max.assistantgpt.BuildConfig
import lol.max.assistantgpt.R
import lol.max.assistantgpt.api.SensorValues
import lol.max.assistantgpt.api.chat.ChatAPI
import lol.max.assistantgpt.api.chat.GPTModel
import lol.max.assistantgpt.api.chat.availableModels
import lol.max.assistantgpt.api.voice.RecognitionListener
import lol.max.assistantgpt.ui.dialog.SensorRequest
import java.lang.reflect.Type
import java.util.*
import kotlin.concurrent.thread

data class ChatScreenUiState(
    val context: Context,
    val chatList: ArrayList<ChatMessage> = arrayListOf(
        ChatMessage(
            ChatMessageRole.SYSTEM.value(),
            context.getString(R.string.gpt_system_prompt)
        )
    ),
    val enableButtons: Boolean = true,
)

class ChatScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChatScreenUiState(application))
    val uiState: StateFlow<ChatScreenUiState> = _uiState.asStateFlow()

    private val sharedPreferences =
        application.applicationContext.getSharedPreferences(
            application.getString(R.string.app_name),
            Context.MODE_PRIVATE
        )

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
        ),
        showFunctions = sharedPreferences.getBoolean("showFunctions", Options.Default.showFunctions)
    )

    private val chatListType: Type = object : TypeToken<SnapshotStateList<Chat>>() {}.type
    var savedChats: SnapshotStateList<Chat> =
        Gson().fromJson(sharedPreferences.getString("savedChats", "[]")!!, chatListType)
        private set

    var currentChatIdx by mutableStateOf(-1)
    val showLoading = MutableTransitionState(false)
    private var newMessageAnimated by mutableStateOf(false)
    fun getAndSetNewMessageAnimated(): Boolean {
        val temp = newMessageAnimated
        newMessageAnimated = true
        return temp
    }

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

    private val tts = TextToSpeech(
        application
    ) { status ->
        if (status == TextToSpeech.SUCCESS)
            Log.i(application.getString(R.string.app_name), "TTS initialized")
        else Log.e(application.getString(R.string.app_name), "TTS failed to initialize")
    }

    private fun speakText(string: String?) {
        if (string != null)
            tts.speak(string, TextToSpeech.QUEUE_FLUSH, null, Random().nextInt().toString())
    }

    private val stt: SpeechRecognizer =
        if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(application))
            SpeechRecognizer.createOnDeviceSpeechRecognizer(application)
        else SpeechRecognizer.createSpeechRecognizer(application)

    var sensorValues = SensorValues()
    var requestPermission: (String) -> Unit = { }

    fun sendChatInput(
        showUserMessage: (String) -> Unit,
    ) {
        if (chatInput == "") return
        val newChatList = _uiState.value.chatList
        newChatList.add(ChatMessage(ChatMessageRole.USER.value(), chatInput.trim()))
        newMessageAnimated = false
        _uiState.update {
            it.copy(
                enableButtons = false,
                chatList = newChatList
            )
        }
        showLoading.targetState = true
        chatInput = ""

        thread {
            chatApi.getCompletion(
                chatMessages = _uiState.value.chatList,
                model = options.model,
                context = getApplication(),
                allowSensors = options.allowSensors,
                sensorRequest = sensorRequest,
                sensorValues = sensorValues,
                showFunctions = options.showFunctions,
                showMessage = showUserMessage,
                requestPermission = requestPermission,
                updateChatMessageList = { list ->
                    newMessageAnimated = false
                    _uiState.update {
                        it.copy(
                            chatList = list,
                        )
                    }
                    if (list.isEmpty() || (list.last().role == ChatMessageRole.ASSISTANT.value() && list.last().functionCall == null)) {
                        _uiState.update {
                            it.copy(
                                enableButtons = true,
                            )
                        }
                        showLoading.targetState = false
                    }
                    if (currentChatIdx != -1)
                        thread { saveChat() }
                    if (list.isNotEmpty() && list.last().content != null && list.last().role == ChatMessageRole.ASSISTANT.value())
                        speakText(list.last().content)
                })
        }
    }

    var isAssistantVoiceOver: Boolean = false
    fun startVoiceChatInput(activity: Activity, onFinished: (String) -> Unit) {
        if (getApplication<Application>().checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(
                getApplication(),
                getApplication<Application>().getString(R.string.microphone_access_required),
                Toast.LENGTH_SHORT
            ).show()
            activity.requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                Random().nextInt(Int.MAX_VALUE)
            )
            return
        }

        stt.setRecognitionListener(
            RecognitionListener(
                { updateChatInput(it) },
                {
                    endVoiceChatInput(it, onFinished)
                })
        )

        _uiState.update {
            it.copy(
                enableButtons = false
            )
        }
        tts.stop()
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

    fun endVoiceChatInput(statusCode: Int, showUserMessage: (String) -> Unit) {
        updateShowDialog(DialogTypes.NONE)
        if (statusCode == 0 && chatInput != "") {
            sendChatInput(showUserMessage)
        } else _uiState.update {
            it.copy(
                enableButtons = true
            )
        }
    }

    fun cancelVoiceChatInput() {
        stt.cancel()
        _uiState.update {
            it.copy(
                enableButtons = true
            )
        }
        chatInput = ""
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
        e.putBoolean("showFunctions", options.showFunctions)
        e.apply()
    }

    fun updateTimeoutSec() {
        chatApi.setTimeoutSec(options.timeoutSec)
    }

    fun saveChat(name: String = "") {
        val gson = Gson()
        if (currentChatIdx == -1) {
            val newChat = Chat(name, UUID.randomUUID().toString())
            savedChats.add(newChat)
            currentChatIdx = savedChats.size - 1
        }

        savedChats[currentChatIdx].numTokens = chatApi.tokensUsed
        sharedPreferences.edit().putString("savedChats", gson.toJson(savedChats)).apply()

        getApplication<Application>().openFileOutput(
            savedChats[currentChatIdx].uuid,
            Context.MODE_PRIVATE
        ).use {
            ObjectMapper().writeValue(it, _uiState.value.chatList)
        }
    }

    fun loadChat(index: Int) {
        getApplication<Application>().openFileInput(savedChats[index].uuid).bufferedReader()
            .use { reader ->
                val type = object : TypeReference<ArrayList<ChatMessage>>() {}
                val messages = ObjectMapper().readValue(reader, type)

                currentChatIdx = index
                _uiState.update {
                    it.copy(
                        chatList = messages,
                        enableButtons = true,
                    )
                }
                newMessageAnimated = true
                showLoading.targetState = false
            }
        chatApi.tokensUsed = savedChats[index].numTokens
        Log.i(null, "Loaded chat ${savedChats[index].uuid} with ${chatApi.tokensUsed} tokens")
    }

    fun deleteChat() {
        if (currentChatIdx != -1) {
            getApplication<Application>().deleteFile(savedChats[currentChatIdx].uuid)
            savedChats.removeAt(currentChatIdx)
            sharedPreferences.edit().putString("savedChats", Gson().toJson(savedChats)).apply()
            currentChatIdx = -1
        }
    }

    fun resetChat() {
        if (_uiState.value.enableButtons) {
            _uiState.update {
                it.copy(
                    chatList = arrayListOf(),
                )
            }
            newMessageAnimated = false
            currentChatIdx = -1
        } else
            Toast.makeText(
                getApplication(),
                "Please wait for the current request to finish.",
                Toast.LENGTH_SHORT
            ).show()
    }
}

data class Chat(
    val name: String,
    val uuid: String,
    var numTokens: Long = 0
)

data class Options(
    var model: GPTModel,
    var timeoutSec: Int,
    var openAiKey: String,
    var googleKey: String,
    var googleSearchId: String,
    var allowSensors: Boolean,
    var confirmSensors: Boolean,
    var showFunctions: Boolean
) {

    companion object {
        val Default = Options(
            model = availableModels[0],
            timeoutSec = 60,
            openAiKey = "",
            googleKey = "",
            googleSearchId = "",
            allowSensors = false,
            confirmSensors = true,
            showFunctions = false
        )
    }
}

enum class DialogTypes {
    NONE, SETTINGS, INFO, VOICE, SENSOR, SENSOR_INFO, SAVE;
}