package lol.max.assistantgpt.ui

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

//@Preview
//@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
//@Composable
//fun PreviewChatScreen() {
//    val input = remember { mutableStateOf("Obmana") }
//    val dialog = remember {
//        mutableStateOf(DialogTypes.NONE)
//    }
//    val snackbarHostState = remember {
//        SnackbarHostState()
//    }
//    val msgLst = arrayListOf<ChatMessage>()
//    msgLst.add(ChatMessage(ChatMessageRole.USER.value(), "Yo waddup bitch"))
//    msgLst.add(ChatMessage(ChatMessageRole.ASSISTANT.value(), "Fuck you"))
//    msgLst.add(ChatMessage(ChatMessageRole.USER.value(), "ay that's not very nice"))
//    msgLst.add(
//        ChatMessage(
//            ChatMessageRole.ASSISTANT.value(),
//            "i dont giva a fuck go fuck yourself"
//        )
//    )
//    msgLst.add(ChatMessage(ChatMessageRole.USER.value(), "why dont you suck my balls"))
//    msgLst.add(ChatMessage(ChatMessageRole.ASSISTANT.value(), "no way they are too ligma"))
//    msgLst.add(ChatMessage(ChatMessageRole.USER.value(), "what is ligma"))
//    msgLst.add(ChatMessage(ChatMessageRole.ASSISTANT.value(), "ligma balls"))
//    msgLst.add(
//        ChatMessage(
//            ChatMessageRole.USER.value(),
//            "wow taht is so mean i will add a bunch of space for testing purposes\n\n\n\n\n\ndumbass\n\n\n\n\n\n\n\ntake that"
//        )
//    )
//
//
//    val onClickSend: () -> Unit =
//        { msgLst.add(ChatMessage(ChatMessageRole.USER.value(), input.value)) }
//    AssistantGPTTheme {
//        Surface {
//            ChatScreen(input, msgLst, showDialog = dialog, snackbarHostState = snackbarHostState, onClickSend = onClickSend, onClickVoice = {})
//        }
//    }
//}
//
//@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
//@Composable
//fun PreviewOptionsDialog(){
//    AssistantGPTTheme {
//        Surface(Modifier.fillMaxSize()) {
//            val type = remember {
//                mutableStateOf(DialogTypes.SETTINGS)
//            }
//            Dialogs(type = type, Options("gpt-3.5-turbo"))
//        }
//    }
//}
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
