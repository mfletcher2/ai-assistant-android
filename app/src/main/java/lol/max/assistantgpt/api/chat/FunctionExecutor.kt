package lol.max.assistantgpt.api.chat

import com.google.gson.Gson
import com.theokanning.openai.completion.chat.ChatFunction
import com.theokanning.openai.completion.chat.ChatFunctionCall
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import lol.max.assistantgpt.ui.dialog.SensorRequest

class FunctionExecutor(functionList: List<ChatFunction>) {
    private val functionMap: HashMap<String, ChatFunction> = hashMapOf()

    val functions: ArrayList<ChatFunction>
        get() = ArrayList(functionMap.values)

    init {
        for (function in functionList) {
            functionMap[function.name] = function
        }
    }

    fun executeAndConvertToMessage(
        functionCall: ChatFunctionCall,
        chatFunctions: ChatFunctions,
        sensorRequest: SensorRequest,
        requestPermission: (String) -> Unit,
        onFinished: (ChatMessage) -> Unit
    ) {
        val gson = Gson()
        val function = functionMap[functionCall.name]
        val arguments =
            gson.fromJson(functionCall.arguments.toPrettyString(), function?.parametersClass)

        try {
            if (!chatFunctions.requiresPermission.containsKey(function)) {
                val chatMessage = ChatMessage(
                    ChatMessageRole.FUNCTION.value(),
                    gson.toJson(function?.executor?.apply(arguments)!!),
                    functionCall.name
                )
                onFinished(chatMessage)

            } else {
                val permissionPair = chatFunctions.requiresPermission[function]!!

                val class1 = arguments as LateResponse
                class1.onSuccess = {
                    val chatMessage = ChatMessage(
                        ChatMessageRole.FUNCTION.value(),
                        gson.toJson(it),
                        functionCall.name
                    )
                    onFinished(chatMessage)
                }

                onGranted =
                    { (function?.executor?.apply(arguments)) as Unit }

                onDenied = {
                    val chatMessage = ChatMessage(
                        ChatMessageRole.FUNCTION.value(),
                        gson.toJson("Permission denied."),
                        functionCall.name
                    )
                    onFinished(chatMessage)
                }
                sensorRequest.permission = permissionPair.first ?: ""
                sensorRequest.sensorName = permissionPair.second
                sensorRequest.onGranted = {
                    if (permissionPair.first != null)
                        requestPermission(permissionPair.first!!)
                    else
                        onGranted()
                }
                sensorRequest.onDenied = { onDenied() }
                sensorRequest.showDialog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val chatMessage = ChatMessage(
                ChatMessageRole.FUNCTION.value(),
                "Error: ${e.message}",
                functionCall.name
            )
            onFinished(chatMessage)
        }
    }

    companion object {
        var onDenied: () -> Unit? = {}
            private set
        var onGranted: () -> Unit? = {}
            private set
    }
}
