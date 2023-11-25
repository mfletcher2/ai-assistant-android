package lol.max.assistantgpt.ui

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import lol.max.assistantgpt.BuildConfig
import lol.max.assistantgpt.R
import lol.max.assistantgpt.api.RecognitionListener
import lol.max.assistantgpt.api.SensorFunctions
import lol.max.assistantgpt.ui.dialog.InfoDialog
import lol.max.assistantgpt.ui.dialog.SaveDialog
import lol.max.assistantgpt.ui.dialog.SensorInfoDialog
import lol.max.assistantgpt.ui.dialog.SensorRequestDialog
import lol.max.assistantgpt.ui.dialog.SettingsDialog
import lol.max.assistantgpt.ui.viewmodel.Chat
import lol.max.assistantgpt.ui.viewmodel.ChatScreenViewModel
import lol.max.assistantgpt.ui.viewmodel.DialogTypes
import java.util.Random
import kotlin.concurrent.thread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    tts: TextToSpeech?,
    stt: SpeechRecognizer?,
    requestPermissionLauncher: ActivityResultLauncher<String>,
    sensorFunctions: SensorFunctions,
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

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
                    sensorFunctions
                ) { str -> speakText(str) }
            })
    )
    ChatNavigationDrawer(
        drawerState,
        viewModel.savedChats,
        { coroutineScope.launch { viewModel.loadChat(it); drawerState.close(); } },
        { viewModel.resetChat(); coroutineScope.launch { drawerState.close() } }) {
        Scaffold(
            bottomBar = {
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
                                sensorFunctions
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
            },
            topBar = {
                ChatTopAppBar(
                    title = if (viewModel.currentChatIdx == -1) stringResource(R.string.app_name_localized) else viewModel.savedChats[viewModel.currentChatIdx].name,
                    drawerState = drawerState,
                    coroutineScope = coroutineScope,
                    newChat = viewModel.currentChatIdx == -1,
                    onSave = {
                        if (viewModel.currentChatIdx == -1) viewModel.updateShowDialog(
                            DialogTypes.SAVE
                        )
                    },
                    onDelete = { viewModel.deleteChat() },
                    enableButton = uiState.enableButtons,
                    updateDialog = { viewModel.updateShowDialog(it) })
            },
            snackbarHost = {
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
                AnimatedVisibility(
                    visibleState = viewModel.showLoading,
                    enter = slideInVertically { fullHeight -> fullHeight },
                    exit = slideOutVertically { fullHeight -> fullHeight }) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
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
            SensorInfoDialog(sensorValues = sensorFunctions.sensorValues) {
                viewModel.updateShowDialog(
                    DialogTypes.NONE
                )
            }

        DialogTypes.SAVE ->
            SaveDialog(
                onDismissRequest = { viewModel.updateShowDialog(DialogTypes.NONE) },
                onConfirm = { thread { viewModel.saveChat(it) } })

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChatTopAppBar(
    title: String,
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    newChat: Boolean,
    enableButton: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    updateDialog: (DialogTypes) -> Unit
) {
    TopAppBar(
        title = {

            Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)

        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ), actions = {
            if (BuildConfig.DEBUG)
                IconButton(onClick = { updateDialog(DialogTypes.SENSOR_INFO) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_sensors_24),
                        contentDescription = "Sensors",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            Box {
                androidx.compose.animation.AnimatedVisibility(
                    visible = newChat,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(onClick = onSave, enabled = enableButton) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_save_as_24),
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = !newChat,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(onClick = onDelete, enabled = enableButton) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            IconButton(onClick = { updateDialog(DialogTypes.INFO) }) {
                Icon(
                    imageVector = Icons.Filled.Info, contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(onClick = { updateDialog(DialogTypes.SETTINGS) }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                coroutineScope.launch {
                    drawerState.apply { if (isClosed) drawerState.open() }
                }
            }) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatNavigationDrawer(
    drawerState: DrawerState,
    chatList: List<Chat>, // name to uuid
    onClickChat: (Int) -> Unit,
    onClickNew: () -> Unit,
    drawerContent: @Composable () -> Unit
) {
    ModalNavigationDrawer(drawerContent = {
        ModalDrawerSheet {
            Text(
                text = "Saved chats",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge
            )
            LazyColumn(content = {
                itemsIndexed(chatList) { idx, chat ->
                    TextButton(
                        { onClickChat(idx) },
                        shape = RoundedCornerShape(bottomEndPercent = 50, topEndPercent = 50)
                    ) {
                        Text(
                            text = chat.name,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                item {
                    TextButton(
                        onClick = onClickNew,
                        shape = RoundedCornerShape(bottomEndPercent = 50, topEndPercent = 50)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add",
                                Modifier.padding(8.dp)
                            )
                            Text(
                                text = "New chat",
                                Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            })
        }
    }, drawerState = drawerState, gesturesEnabled = true) {
        drawerContent()
    }
}