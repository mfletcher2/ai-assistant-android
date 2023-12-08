package lol.max.assistantgpt.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring.DampingRatioLowBouncy
import androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lol.max.assistantgpt.BuildConfig
import lol.max.assistantgpt.R
import lol.max.assistantgpt.api.SensorValues
import lol.max.assistantgpt.ui.dialog.*
import lol.max.assistantgpt.ui.viewmodel.Chat
import lol.max.assistantgpt.ui.viewmodel.ChatScreenViewModel
import lol.max.assistantgpt.ui.viewmodel.DialogTypes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sensorValues: SensorValues,
    startVoiceInput: Boolean = false,
    requestPermission: (String) -> Unit,
    viewModel: ChatScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    lateinit var activity: ComponentActivity
    if (LocalContext.current is ComponentActivity) activity = LocalContext.current as ComponentActivity

    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    fun showSnackbar(message: String) {
        coroutineScope.launch { snackBarHostState.showSnackbar(message) }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    viewModel.sensorValues = sensorValues
    viewModel.requestPermission = requestPermission

    val transitionState = remember { MutableTransitionState(false) }.apply { targetState = true }
    var showChat by remember { mutableStateOf(true) }

    LaunchedEffect(startVoiceInput) {
        if (startVoiceInput && !viewModel.isAssistantVoiceOver) {
            viewModel.isAssistantVoiceOver = true
            viewModel.startVoiceChatInput(activity)
        }
    }

    ChatNavigationDrawer(
        drawerState = drawerState,
        chatList = viewModel.savedChats,
        onClickChat = {
            showChat = false; coroutineScope.launch(Dispatchers.IO) {
            viewModel.loadChat(it); drawerState.close(); showChat = true
        }
        },
        onClickNew = { viewModel.resetChat(); coroutineScope.launch { drawerState.close() } }) {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = slideInVertically(spring(stiffness = StiffnessLow)) { fullHeight -> fullHeight }) {
                    BottomAppBar(modifier = Modifier.height(100.dp)) {
                        ChatInput(
                            inputText = viewModel.chatInput,
                            onInputTextChanged = { viewModel.updateChatInput(it) },
                            enableButton = uiState.enableButtons,
                            onClickSend = {
                                viewModel.sendChatInput { showSnackbar(it) }
                            },
                            onClickVoice = {
                                viewModel.startVoiceChatInput(activity)
                            }
                        )
                    }
                }
            },
            topBar = {
                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = slideInVertically(spring(stiffness = StiffnessLow)) { fullHeight -> -fullHeight }) {
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
                        onDelete = {
                            val temp = viewModel.savedChats[viewModel.currentChatIdx].name
                            viewModel.deleteChat()
                            coroutineScope.launch {
                                snackBarHostState.showSnackbar("Deleted $temp")
                            }
                        },
                        enableButton = uiState.enableButtons,
                        updateDialog = { viewModel.updateShowDialog(it) })
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackBarHostState)
            }) {
            AnimatedVisibility(
                visible = showChat,
                enter = fadeIn() + slideInVertically(
                    spring(
                        DampingRatioLowBouncy,
                        StiffnessLow
                    )
                ),
                exit = ExitTransition.None
            ) {
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
                        showFunctions = viewModel.options.showFunctions,
                        isAnimated = { viewModel.getAndSetNewMessageAnimated() }
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
    }
    when (viewModel.showDialog) {
        DialogTypes.SETTINGS -> SettingsDialog(options = viewModel.options, {
            viewModel.updateShowDialog(
                DialogTypes.NONE
            )
        }, { viewModel.updateTimeoutSec(); viewModel.saveSharedPreferences() })

        DialogTypes.INFO -> InfoDialog { viewModel.updateShowDialog(DialogTypes.NONE) }
        DialogTypes.VOICE -> Dialog(onDismissRequest = {
            viewModel.cancelVoiceChatInput()
            viewModel.updateShowDialog(DialogTypes.NONE)
        }) {
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
            SensorInfoDialog(sensorValues = sensorValues) {
                viewModel.updateShowDialog(
                    DialogTypes.NONE
                )
            }

        DialogTypes.SAVE ->
            SaveDialog(
                onDismissRequest = { viewModel.updateShowDialog(DialogTypes.NONE) },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        viewModel.saveChat(it.trim())
                        snackBarHostState.showSnackbar("Saved ${viewModel.savedChats[viewModel.currentChatIdx].name}")
                    }
                })

        else -> return
    }

}

@Composable
fun MessageCard(msg: ChatMessage) {
    if (msg.content == null) return
    val msgCorner = 12.dp
    val context = LocalContext.current
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
                MarkdownText(
                    markdown = msg.content
                        ?: (msg.functionCall.name + msg.functionCall.arguments.toPrettyString()),
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (msg.role) {
                        ChatMessageRole.ASSISTANT.value() -> MaterialTheme.colorScheme.onPrimaryContainer
                        ChatMessageRole.USER.value() -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    isTextSelectable = true,
                    linkColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onLinkClicked = {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(it)
                        Log.i("AssistantGPT", "Opening link: $it")
                        if (intent.resolveActivity(context.packageManager) == null) {
                            Toast.makeText(context, "No app found to open link.", Toast.LENGTH_SHORT).show()
                            Log.e("AssistantGPT", "No app found to open $it")
                        } else context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun Conversation(
    messages: List<ChatMessage>,
    showFunctions: Boolean,
    isAnimated: () -> Boolean
) {
    val listState = rememberLazyListState()
    if (messages.isNotEmpty())
        LaunchedEffect(messages.size) {
            listState.animateScrollToItem(messages.size - 1)
        }
    LazyColumn(state = listState) {
        itemsIndexed(messages) { i, message ->
            val state = remember {
                MutableTransitionState(i != messages.size - 1 || isAnimated()).apply {
                    // Start the animation immediately.
                    targetState = true
                }
            }

            if (message.content != null &&
                (message.role == ChatMessageRole.ASSISTANT.value() || message.role == ChatMessageRole.USER.value()
                        || (message.role == ChatMessageRole.FUNCTION.value() && showFunctions))
            ) {
                AnimatedVisibility(
                    visibleState = state,
                    enter = fadeIn() + slideInVertically(spring(stiffness = StiffnessMediumLow)) { it }) {
                    MessageCard(msg = message)
                }
            }


        }
    }
}

@Composable
fun ChatMessageConversation(
    chatMessages: List<ChatMessage>,
    showFunctions: Boolean,
    isAnimated: () -> Boolean
) {
    Conversation(
        messages = chatMessages,
        showFunctions = showFunctions,
        isAnimated = isAnimated
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
            enabled = enableButton,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions { onClickSend() },
            trailingIcon = {
                AnimatedVisibility(visible = inputText.isNotEmpty() && enableButton) {
                    IconButton(
                        onClick = onClickSend,
                        enabled = enableButton,
                    ) {
                        Icon(imageVector = Icons.Rounded.Send, contentDescription = "Send")
                    }
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
            AnimatedContent(
                targetState = title,
                label = "title",
                transitionSpec = {
                    (slideInHorizontally(spring(DampingRatioMediumBouncy)) { width -> -width } + fadeIn() with slideOutHorizontally { width -> width } + fadeOut()) using SizeTransform(
                        false
                    )
                }) { newTitle ->
                Text(text = newTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
            AnimatedContent(
                targetState = newChat, transitionSpec = { fadeIn() + scaleIn() with fadeOut() + scaleOut() },
                label = "Save button"
            ) {
                IconButton(onClick = { if (it) onSave() else onDelete() }, enabled = enableButton) {
                    if (it)
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_save_as_24),
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    else
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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