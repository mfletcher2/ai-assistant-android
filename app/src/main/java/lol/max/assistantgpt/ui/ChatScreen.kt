package lol.max.assistantgpt.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import lol.max.assistantgpt.R
import lol.max.assistantgpt.api.ChatAPI
import lol.max.assistantgpt.api.RecognitionListener
import lol.max.assistantgpt.ui.viewmodel.ChatScreenViewModel
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ChatScreen(
    tts: TextToSpeech,
    stt: SpeechRecognizer,
    chatAPI: ChatAPI,
    options: Options,
    viewModel: ChatScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val random = Random()
    val activity = LocalContext.current as Activity

    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    fun speakText(string: String) {
        tts.speak(string, TextToSpeech.QUEUE_FLUSH, null, random.nextInt().toString())
    }
    stt.setRecognitionListener(
        RecognitionListener({ viewModel.updateChatInput(it) },
            {
                viewModel.endVoiceChatInput(
                    it,
                    coroutineScope,
                    chatAPI,
                    options,
                    snackBarHostState
                ) { str -> speakText(str) }
            })
    )

    Scaffold(bottomBar = {
        BottomAppBar(modifier = Modifier.height(100.dp)) {
            ChatInput(
                inputText = viewModel.chatInput,
                onInputTextChanged = { viewModel.updateChatInput(it) },
                enableButton = uiState.enableButtons,
                onClickSend = {
                    viewModel.sendChatInput(
                        coroutineScope,
                        chatAPI,
                        options,
                        snackBarHostState,
                    ) {
                        speakText(it)
                    }
                },
                onClickVoice = {
                    if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PERMISSION_DENIED) {
                        Toast.makeText(
                            activity,
                            "Microphone access is required",
                            Toast.LENGTH_SHORT
                        ).show()
                        activity.requestPermissions(
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            random.nextInt(Int.MAX_VALUE)
                        )
                        return@ChatInput
                    }
                    viewModel.startChatVoiceInput(stt)
                }
            )
        }
    }, topBar = {
        TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.primaryContainer
            ), actions = {
                IconButton(onClick = { viewModel.updateShowDialog(DialogTypes.SETTINGS) }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            })
    }, snackbarHost = {
        SnackbarHost(hostState = snackBarHostState)
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
                chatMessages = uiState.chatList,
                showLoading = uiState.enableWaitingIndicator
            )
        }
        SettingsDialog(type = viewModel.showDialog, options = options) {
            viewModel.updateShowDialog(
                DialogTypes.NONE
            )
        }
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

            if (message.content != null && (message.role == ChatMessageRole.ASSISTANT.value() || message.role == ChatMessageRole.USER.value())) {
                AnimatedVisibility(
                    visibleState = state,
                    enter = fadeIn() + slideInVertically { it }) {
                    MessageCard(msg = message)
                }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ChatInput(
    modifier: Modifier = Modifier,
    inputText: String,
    onInputTextChanged: (String) -> Unit,
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
            value = inputText,
            onValueChange = { onInputTextChanged(it) },
            colors = TextFieldDefaults.textFieldColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
                .onKeyEvent {
                    if (it.key == Key.Enter) {
                        onClickSend()
                    }
                    false
                },
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