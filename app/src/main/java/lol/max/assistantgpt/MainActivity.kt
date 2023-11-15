package lol.max.assistantgpt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.coroutines.launch
import lol.max.assistantgpt.ui.theme.AssistantGPTTheme
import java.util.Random
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private var prefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var ttsInit = false
        val tts = TextToSpeech(
            this
        ) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.i(getString(R.string.app_name), "TTS initialized")
                ttsInit = true
            } else
                Log.e(getString(R.string.app_name), "TTS failed to initialize")
        }
        val stt = SpeechRecognizer.createSpeechRecognizer(this)

        prefs = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        val chatApi = ChatAPI(BuildConfig.OPENAI_API_KEY)

        val random = Random()

        val viewModel: ChatMessageListViewModel by viewModels()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    // Update UI elements
                    setContent {
                        AssistantGPTTheme {
                            var enableButton by rememberSaveable { mutableStateOf(true) }
                            var waitingForResponse by rememberSaveable { mutableStateOf(false) }
                            val input = rememberSaveable { mutableStateOf("") }
                            val showDialog =
                                rememberSaveable { mutableStateOf(DialogTypes.NONE) }
                            val options = Options(
                                model = prefs!!.getString("model", "gpt-3.5-turbo")!!)

                            fun sendMessage() {
                                if (input.value == "") return
                                enableButton = false
                                waitingForResponse = true
                                viewModel.addMessage(
                                    ChatMessage(
                                        ChatMessageRole.USER.value(),
                                        input.value.trim()
                                    )
                                )
                                input.value = ""
                                thread {
                                    viewModel.addMessages(chatApi.getCompletion(viewModel.getMessages(), options.model))
                                    enableButton = true
                                    waitingForResponse = false
                                    if (ttsInit)
                                        tts.speak(
                                            viewModel.getMessages()[viewModel.getMessages().size - 1].content,
                                            TextToSpeech.QUEUE_FLUSH,
                                            null,
                                            random.nextInt().toString()
                                        )
                                }
                            }

                            fun onClickVoice() {//onClickVoice: the voice input button is pressed
                                if (!requestSpeechPermission()) return
                                enableButton = false
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

                            stt.setRecognitionListener(RecognitionListener(input) { statusCode ->
                                if (input.value != "" && statusCode == 0) sendMessage()
                                else enableButton = true
                            })

                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                ChatScreen(input,
                                    viewModel.getMessages(),
                                    enableButton,
                                    waitingForResponse,
                                    showDialog,
                                    { sendMessage() },
                                    { onClickVoice() })
                                Dialogs(type = showDialog, options = options)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestSpeechPermission(): Boolean {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            return true
        else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 12)
        }
        return false
    }
}

@Composable
fun MessageCard(msg: ChatMessage) {
    val msgCorner = 12.dp
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Icon(
            painterResource(id = R.drawable.baseline_assistant_24),
            contentDescription = "Profile image",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(40.dp)
                .alpha(if (msg.role == ChatMessageRole.ASSISTANT.value()) 1f else 0f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            horizontalAlignment = if (msg.role == ChatMessageRole.ASSISTANT.value()) Alignment.Start else Alignment.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = msg.role[0].uppercaseChar() + msg.role.substring(1),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = if (msg.role == ChatMessageRole.ASSISTANT.value()) RoundedCornerShape(
                    msgCorner,
                    msgCorner,
                    msgCorner,
                    0.dp
                )
                else RoundedCornerShape(msgCorner, msgCorner, 0.dp, msgCorner),
                color = if (msg.role == ChatMessageRole.ASSISTANT.value()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            ) {
                Text(
                    text = msg.content,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun Conversation(
    messages: List<ChatMessage>,
    showLoading: Boolean = false,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(messages.size)
    }
    LazyColumn(state = listState) {
        itemsIndexed(messages) { i, message ->
            val state = remember {
                MutableTransitionState(i != messages.size - 1).apply {
                    // Start the animation immediately.
                    targetState = true
                }
            }

            AnimatedVisibility(
                visibleState = state,
                enter = fadeIn() + slideInVertically { it }) {
                MessageCard(msg = message)
            }


        }
        item {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(if (showLoading) 75.dp else 0.dp)
                    .padding(16.dp)
            )

        }
    }
}

@Composable
fun ChatMessageConversation(
    chatMessages: List<ChatMessage>,
    showLoading: Boolean = false,
) {
    Conversation(messages = chatMessages, showLoading = showLoading)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInput(
    modifier: Modifier = Modifier,
    inputText: MutableState<String>,
    enableButton: Boolean = true,
    onClickSend: () -> Unit,
    onClickVoice: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        OutlinedTextField(
            value = inputText.value,
            onValueChange = { newText: String -> inputText.value = newText.trimStart() },
            colors = TextFieldDefaults.textFieldColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions { onClickSend() }
        )
        FilledIconButton(
            onClick = onClickSend,
            modifier = Modifier
                .padding(8.dp)
                .size(50.dp),
            enabled = enableButton,
            shape = RoundedCornerShape(25)
        ) {
            Icon(imageVector = Icons.Rounded.Send, contentDescription = "Send")
        }
        FilledIconButton(
            onClick = { onClickVoice() },
            modifier = Modifier
                .padding(8.dp)
                .size(50.dp),
            enabled = enableButton,
            shape = RoundedCornerShape(25)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_mic_24),
                contentDescription = "Send"
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ChatScreen(
    input: MutableState<String>,
    msgLst: List<ChatMessage>,
    enableButton: Boolean = true,
    showLoading: Boolean = false,
    showDialog: MutableState<DialogTypes>,
    onClickSend: () -> Unit,
    onClickVoice: () -> Unit
) {
    Scaffold(bottomBar = {
        BottomAppBar(modifier = Modifier.height(100.dp)) {
            ChatInput(
                inputText = input,
                enableButton = enableButton,
                onClickSend = { onClickSend() },
                onClickVoice = { onClickVoice() })
        }
    }, topBar = {
        TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.primaryContainer
            ), actions = {
                IconButton(onClick = { showDialog.value = DialogTypes.SETTINGS }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            })
    }) {
        Box(
            modifier = Modifier
                .padding(
                    PaddingValues(
                        bottom = it.calculateBottomPadding(),
                        top = it.calculateTopPadding()
                    )
                )
                .fillMaxSize(), contentAlignment = Alignment.BottomCenter
        ) {
            ChatMessageConversation(
                chatMessages = msgLst,
                showLoading = showLoading
            )
        }
    }
}


