package lol.max.assistantgpt.api.chat

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.service.OpenAiService.*
import lol.max.assistantgpt.api.SensorValues
import lol.max.assistantgpt.ui.dialog.SensorRequest
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.time.Duration
import kotlin.concurrent.thread


class ChatAPI {

    var tokensUsed: Long = 0

    fun getCompletion(
        chatMessages: ArrayList<ChatMessage>,
        model: GPTModel,
        context: Context,
        allowSensors: Boolean,
        sensorRequest: SensorRequest,
        sensorValues: SensorValues,
        showMessage: (String) -> Unit,
        showFunctions: Boolean,
        apiKey: String,
        googleKey: String,
        timeoutSec: Int,
        requestPermission: (String) -> Unit,
        updateChatMessageList: (ArrayList<ChatMessage>) -> Unit
    ) {
        val mapper: ObjectMapper = defaultObjectMapper()
        val client: OkHttpClient =
            defaultClient(apiKey, Duration.ofSeconds(timeoutSec.toLong()))
                .newBuilder()
                .build()
        val retrofit: Retrofit = defaultRetrofit(client, mapper)

        val api: OpenAiApi = retrofit.create(OpenAiApi::class.java)
        val service = OpenAiService(api)

        val encodingRegistry = Encodings.newLazyEncodingRegistry()


        var messagesListCopy = ArrayList(chatMessages)

        messagesListCopy = countTokensAndTruncate(
            messagesListCopy,
            encodingRegistry.getEncodingForModel(model.name).get(),
            model.maxTokens
        )

        val chatFunctions = ChatFunctions(context, sensorValues, googleKey)
        val functionExecutor = FunctionExecutor(chatFunctions.getFunctionList(allowSensors))

        val chatCompletionRequest = ChatCompletionRequest.builder()
            .messages(messagesListCopy)
            .model(model.name)
            .n(1)
            .temperature(0.5)
            .maxTokens(256)
            .functions(functionExecutor.functions)
            .build()

        try {
            val responseRequest = service.createChatCompletion(chatCompletionRequest)
            tokensUsed = responseRequest.usage.totalTokens

            val responseMessage = responseRequest.choices[0].message
            Log.i("AssistantGPT", "GPT responded: ${responseMessage.content}")
            Log.i("AssistantGPT", "with stop reason: ${responseRequest.choices[0].finishReason}")
            messagesListCopy.add(responseMessage)

            val functionCall = responseMessage.functionCall
            if (functionCall != null) {
                showMessage(
                    "Executing ${functionCall.name}${
                        functionCall.arguments.toPrettyString().replace('{', '(').replace('}', ')')
                    }"
                )
                Log.i(
                    "AssistantGPT",
                    "GPT is running this function: ${functionCall.name}${functionCall.arguments}"
                )
                functionExecutor.executeAndConvertToMessage(
                    functionCall = responseMessage.functionCall,
                    chatFunctions = chatFunctions,
                    sensorRequest = sensorRequest,
                    requestPermission = requestPermission,
                ) {
                    thread {
                        Log.i(
                            "AssistantGPT",
                            "Function response: ${it.content}"
                        )
                        messagesListCopy.add(it)
                        updateChatMessageList(messagesListCopy)
                        getCompletion(
                            chatMessages = messagesListCopy,
                            model = model,
                            context = context,
                            allowSensors = allowSensors,
                            sensorRequest = sensorRequest,
                            sensorValues = sensorValues,
                            showMessage = showMessage,
                            apiKey = apiKey,
                            googleKey = googleKey,
                            timeoutSec = timeoutSec,
                            requestPermission = requestPermission,
                            showFunctions = showFunctions,
                            updateChatMessageList = updateChatMessageList
                        )
                    }
                }
            } else updateChatMessageList(messagesListCopy)
        } catch (e: RuntimeException) {
            e.printStackTrace()
            e.message?.let { showMessage(it) }
            removeUnusedMessages(messagesListCopy)
            updateChatMessageList(messagesListCopy)
        }
    }

    private fun removeUnusedMessages(list: ArrayList<ChatMessage>) {
        while (list.size > 0 && list.last().role != ChatMessageRole.ASSISTANT.value())
            list.removeLast()
    }

    private fun countTokensAndTruncate(
        list: ArrayList<ChatMessage>,
        encoding: Encoding,
        maxTokens: Int
    ): ArrayList<ChatMessage> {
        val numTokens = tokensUsed + encoding.countTokens(list.last().content)
        Log.i("ChatAPI", "Number of tokens: $numTokens")
        if (numTokens > maxTokens && list.size > 2) {
            Log.i("ChatAPI", "Too many tokens, truncating messages")
            list.removeAt(1)
            tokensUsed -= encoding.countTokens(list[1].content)
            countTokensAndTruncate(list, encoding, maxTokens)
        }
        return list
    }
}

data class GPTModel(val name: String, val maxTokens: Int)

val availableModels = listOf(
    GPTModel("gpt-3.5-turbo", 3000),
    GPTModel("gpt-4", 3000)
)

