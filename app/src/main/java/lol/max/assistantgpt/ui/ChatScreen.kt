package lol.max.assistantgpt.ui

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import lol.max.assistantgpt.BuildConfig
import lol.max.assistantgpt.R
import lol.max.assistantgpt.api.RecognitionListener
import lol.max.assistantgpt.api.SensorValues
import lol.max.assistantgpt.ui.dialog.DialogTypes
import lol.max.assistantgpt.ui.dialog.InfoDialog
import lol.max.assistantgpt.ui.dialog.SensorInfoDialog
import lol.max.assistantgpt.ui.dialog.SensorRequestDialog
import lol.max.assistantgpt.ui.dialog.SettingsDialog
import lol.max.assistantgpt.ui.viewmodel.ChatScreenViewModel
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    tts: TextToSpeech?,
    stt: SpeechRecognizer?,
    requestPermissionLauncher: ActivityResultLauncher<String>,
    sensorValues: MutableState<SensorValues>,
    viewModel: ChatScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val random = Random()
    lateinit var activity: ComponentActivity
    if (LocalContext.current is ComponentActivity)
        activity = LocalContext.current as ComponentActivity
    viewModel.requestPermissionLauncher = requestPermissionLauncher

    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    val micReq = stringResource(id = R.string.microphone_access_required)

    fun speakText(string: String?) {
        if (string != null)
            tts?.speak(string, TextToSpeech.QUEUE_FLUSH, null, random.nextInt().toString())
    }
    stt?.setRecognitionListener(
        RecognitionListener(
            { viewModel.updateChatInput(it) },
            {
                viewModel.endVoiceChatInput(
                    activity,
                    it,
                    coroutineScope,
                    snackBarHostState,
                    sensorValues.value
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
                        activity,
                        snackBarHostState,
                        coroutineScope,
                        sensorValues.value
                    ) {
                        if (it.role == ChatMessageRole.ASSISTANT.value())
                            speakText(it.content)
                    }
                },
                onClickVoice = {
                    if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PERMISSION_DENIED) {
                        Toast.makeText(
                            activity,
                            micReq,
                            Toast.LENGTH_SHORT
                        ).show()
                        activity.requestPermissions(
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            random.nextInt(Int.MAX_VALUE)
                        )
                        return@ChatInput
                    }
                    if (stt != null) {
                        tts?.stop()
                        viewModel.startChatVoiceInput(stt)
                    }
                }
            )
        }
    }, topBar = {
        TopAppBar(
            title = { Text(text = stringResource(R.string.app_name_localized)) },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            ), actions = {
                if (BuildConfig.DEBUG)
                    IconButton(onClick = { viewModel.updateShowDialog(DialogTypes.SENSOR_INFO) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_sensors_24),
                            contentDescription = "Sensors",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                IconButton(onClick = { viewModel.updateShowDialog(DialogTypes.INFO) }) {
                    Icon(
                        imageVector = Icons.Filled.Info, contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = { viewModel.updateShowDialog(DialogTypes.SETTINGS) }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onPrimary
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
                newMessageAnimated = uiState.newMessageAnimated,
                showFunctions = viewModel.options.showFunctions
            )
            if (uiState.enableWaitingIndicator)
                LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
    when (viewModel.showDialog) {
        DialogTypes.SETTINGS -> SettingsDialog(options = viewModel.options, {
            viewModel.updateShowDialog(
                DialogTypes.NONE
            )
        }, { viewModel.updateTimeoutSec(); viewModel.saveSharedPreferences() })

        DialogTypes.INFO -> InfoDialog { viewModel.updateShowDialog(DialogTypes.NONE) }
        DialogTypes.VOICE -> Dialog(onDismissRequest = {}) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_mic_24),
                contentDescription = "Microphone",
                modifier = Modifier.size(128.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DialogTypes.SENSOR ->
            SensorRequestDialog(
                sensorRequest = viewModel.sensorRequest,
                !viewModel.options.confirmSensors
            )

        DialogTypes.SENSOR_INFO ->
            SensorInfoDialog(sensorValues = sensorValues) { viewModel.updateShowDialog(DialogTypes.NONE) }

        else -> return
    }

}

@Composable
fun MessageCard(msg: ChatMessage) {
    if (msg.content == null) return
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
            horizontalAlignment = when (msg.role) {
                ChatMessageRole.ASSISTANT.value() -> Alignment.Start
                ChatMessageRole.USER.value() -> Alignment.End
                else -> Alignment.CenterHorizontally
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (msg.role != ChatMessageRole.FUNCTION.value()) msg.role[0].uppercaseChar() + msg.role.substring(
                    1
                )
                else msg.name,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = when (msg.role) {
                    ChatMessageRole.ASSISTANT.value() -> RoundedCornerShape(
                        msgCorner,
                        msgCorner,
                        msgCorner,
                        0.dp
                    )

                    ChatMessageRole.USER.value() -> RoundedCornerShape(
                        msgCorner,
                        msgCorner,
                        0.dp,
                        msgCorner
                    )

                    else -> RoundedCornerShape(msgCorner)
                },
                color = when (msg.role) {
                    ChatMessageRole.ASSISTANT.value() -> MaterialTheme.colorScheme.primaryContainer
                    ChatMessageRole.USER.value() -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                }
            ) {
                SelectionContainer {
                    Text(
                        text = msg.content
                            ?: (msg.functionCall.name + msg.functionCall.arguments.toPrettyString()),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun Conversation(
    messages: List<ChatMessage>,
    newMessageAnimated: MutableState<Boolean> = mutableStateOf(true),
    showFunctions: Boolean
) {
    val listState = rememberLazyListState()
    if (messages.isNotEmpty())
        LaunchedEffect(messages.size) {
            listState.animateScrollToItem(messages.size - 1)
        }
    LazyColumn(state = listState) {
        itemsIndexed(messages) { i, message ->
            val state = remember {
                MutableTransitionState(i != messages.size - 1 || newMessageAnimated.value).apply {
                    // Start the animation immediately.
                    targetState = true
                    newMessageAnimated.value = true
                }
            }

            if (message.content != null &&
                (message.role == ChatMessageRole.ASSISTANT.value() || message.role == ChatMessageRole.USER.value()
                        || (message.role == ChatMessageRole.FUNCTION.value() && showFunctions))
            ) {
                AnimatedVisibility(
                    visibleState = state,
                    enter = fadeIn() + slideInVertically { it }) {
                    MessageCard(msg = message)
                }
            }


        }
    }
}

@Composable
fun ChatMessageConversation(
    chatMessages: List<ChatMessage>,
    newMessageAnimated: MutableState<Boolean> = mutableStateOf(true),
    showFunctions: Boolean
) {
    Conversation(
        messages = chatMessages,
        newMessageAnimated = newMessageAnimated,
        showFunctions = showFunctions
    )
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
            onValueChange = { if (enableButton) onInputTextChanged(it) },
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
            keyboardActions = KeyboardActions { onClickSend() },
            trailingIcon = {
                IconButton(
                    onClick = onClickSend,
                    enabled = enableButton,
                ) {
                    Icon(imageVector = Icons.Rounded.Send, contentDescription = "Send")
                }
            }
        )
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