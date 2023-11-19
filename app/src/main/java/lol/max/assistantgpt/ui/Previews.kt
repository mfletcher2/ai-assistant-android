package lol.max.assistantgpt.ui

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import lol.max.assistantgpt.ui.theme.AssistantGPTTheme
import lol.max.assistantgpt.ui.viewmodel.ChatScreenViewModel
import lol.max.assistantgpt.ui.viewmodel.Options

//@Preview(name = "Light Mode", showBackground = true)
//@Preview(
//    uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, name = "Dark Mode"
//)
//@Composable
//fun PreviewMessageCard() {
//    AssistantGPTTheme {
//        Surface {
//            MessageCard(Message("Ligma", "I love jetpack compose!"))
//        }
//    }
//}
//
////@Preview
//@Composable
//fun PreviewConversation() {
//    AssistantGPTTheme {
//        Surface {
//            Conversation(messages = SampleDataSet.SampleData.conversationSample)
//        }
//    }
//}
//
////@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
//@Composable
//fun PreviewChatMessageConversation() {
//    val msgLst = arrayListOf<ChatMessage>()
//    msgLst.add(ChatMessage(ChatMessageRole.USER.value(), "Yo waddup bitch"))
//    msgLst.add(ChatMessage(ChatMessageRole.ASSISTANT.value(), "Fuck you"))
//    AssistantGPTTheme {
//        Surface {
//            ChatMessageConversation(chatMessages = msgLst)
//        }
//    }
//}
//
////@Preview
//@Composable
//fun PreviewChatInput() {
//    val context = LocalContext.current
//    val input = remember { mutableStateOf("Obmana") }
//    AssistantGPTTheme {
//        Surface {
//            ChatInput(inputText = input) {
//                Toast.makeText(context, "You said $input", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewChatScreen() {
    val viewModel: ChatScreenViewModel = viewModel()
    val uiState = viewModel.uiState.collectAsState()

    val msgLst = arrayListOf<ChatMessage>()
    msgLst.add(ChatMessage(ChatMessageRole.USER.value(), "Yo waddup"))
    msgLst.add(ChatMessage(ChatMessageRole.ASSISTANT.value(), "No"))
    msgLst.add(ChatMessage(ChatMessageRole.USER.value(), "that's not very nice"))
    msgLst.add(
        ChatMessage(
            ChatMessageRole.ASSISTANT.value(),
            "i dont care"
        )
    )
    msgLst.add(ChatMessage(ChatMessageRole.USER.value(), "why dont you stop"))
    msgLst.add(ChatMessage(ChatMessageRole.ASSISTANT.value(), "no way"))
    msgLst.add(ChatMessage(ChatMessageRole.USER.value(), "what"))
    msgLst.add(ChatMessage(ChatMessageRole.ASSISTANT.value(), "no"))
    msgLst.add(
        ChatMessage(
            ChatMessageRole.USER.value(),
            "wow taht is so mean i will add a bunch of space for testing purposes\n\n\n\n\n\nyeah\n\n\n\n\n\n\n\ntake that"
        )
    )

    viewModel.setMessagesList(msgLst)

    AssistantGPTTheme {
        Surface {
            ChatScreen(null, null, viewModel = viewModel)
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewOptionsDialog() {
    AssistantGPTTheme {
        SettingsDialog(Options.Default, {})
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInfoDialog() {
    AssistantGPTTheme {
        InfoDialog()
    }
}
//
//
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    var funi by remember { mutableStateOf(name) }
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        Text(
//            text = "Hello ${funi}!", modifier = modifier
//        )
//        Image(
//            painter = painterResource(id = R.drawable.ic_launcher_foreground),
//            contentDescription = "ligma balls",
//        )
//        Button(onClick = { funi += " balls" }, modifier = Modifier.padding(16.dp)) {
//            Text(text = "Click for funnies! Do it!")
//        }
//    }
//}
//
////    @Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    AssistantGPTTheme {
//        Greeting("Ligma", Modifier.padding(24.dp))
//    }
//}
//
//@OptIn(ExperimentalFoundationApi::class)
//@Preview(showBackground = true)
//@Composable
//fun AnimationTestPreview() {
//    var list by remember { mutableStateOf(listOf("A", "B", "C")) }
//    LazyColumn {
//        item {
//            Button(onClick = { list = list.shuffled() }) {
//                Text("Shuffle")
//                Text("Ligma balls")
//            }
//        }
//        item {
//            FilledIconButton(
//                onClick = { },
//            ) {
//                Icon(Icons.Filled.Build, contentDescription = "")
//            }
//        }
//        items(list, key = { it }) {
//            Text("Item $it", Modifier.animateItemPlacement())
//        }
//    }
//
//}
