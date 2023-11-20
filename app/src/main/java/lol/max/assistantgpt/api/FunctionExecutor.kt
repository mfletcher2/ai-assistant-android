package lol.max.assistantgpt.api

import androidx.activity.result.ActivityResultLauncher
import com.google.gson.Gson
import com.theokanning.openai.completion.chat.ChatFunction
import com.theokanning.openai.completion.chat.ChatFunctionCall
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole

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
        functions: Functions,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        onFinished: (ChatMessage) -> Unit
    ) {
        val gson = Gson()
        val function = functionMap[functionCall.name]
        val arguments =
            gson.fromJson(functionCall.arguments.toPrettyString(), function?.parametersClass)

        if (!functions.requiresPermission.containsKey(function)) {
            val chatMessage = ChatMessage(
                ChatMessageRole.FUNCTION.value(),
                gson.toJson(function?.executor?.apply(arguments)!!),
                functionCall.name
            )
            onFinished(chatMessage)

        } else {
            val permission = functions.requiresPermission[function]

            val class1 = arguments as Functions.LateResponse
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

            requestPermissionLauncher.launch(permission)
        }
    }

    companion object {
        var onDenied: () -> Unit? = {}
        var onGranted: () -> Unit? = {}
    }

}