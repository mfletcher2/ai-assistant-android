package lol.max.assistantgpt.api

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.fasterxml.jackson.databind.ObjectMapper
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.service.OpenAiService.defaultClient
import com.theokanning.openai.service.OpenAiService.defaultObjectMapper
import com.theokanning.openai.service.OpenAiService.defaultRetrofit
import lol.max.assistantgpt.ui.SensorRequest
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.time.Duration
import kotlin.concurrent.thread


class ChatAPI(
    private var apiKey: String,
    private var timeoutSec: Int = 60,
) {
    private var mapper: ObjectMapper = defaultObjectMapper()
    private var client: OkHttpClient =
        defaultClient(apiKey, Duration.ofSeconds(timeoutSec.toLong()))
            .newBuilder()
            .build()
    private var retrofit: Retrofit = defaultRetrofit(client, mapper)

    private var api: OpenAiApi = retrofit.create(OpenAiApi::class.java)
    private var service: OpenAiService = OpenAiService(api)

    private var encodingRegistry = Encodings.newLazyEncodingRegistry()

    private var tokensUsed: Long = 0

    fun getCompletion(
        chatMessages: ArrayList<ChatMessage>,
        model: GPTModel,
        context: ComponentActivity,
        allowSensors: Boolean,
        permissionRequestLauncher: ActivityResultLauncher<String>,
        sensorRequest: SensorRequest,
        showMessage: (String) -> Unit,
        updateChatMessageList: (ArrayList<ChatMessage>) -> Unit
    ) {
        val messagesListCopy = ArrayList(chatMessages)

        countTokensAndTruncate(
            messagesListCopy,
            encodingRegistry.getEncodingForModel(model.name).get(),
            model.maxTokens
        )

        val functionsObj = Functions(context)
        val functionExecutor = FunctionExecutor(functionsObj.getFunctionList(allowSensors))

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
                    functions = functionsObj,
                    permissionRequestLauncher = permissionRequestLauncher,
                    sensorRequest = sensorRequest
                ) {
                    thread {
                        Log.i(
                            "AssistantGPT",
                            "Function response: ${it.content}"
                        )
                        messagesListCopy.add(it)
                        getCompletion(
                            chatMessages = messagesListCopy,
                            model = model,
                            context = context,
                            allowSensors = allowSensors,
                            permissionRequestLauncher = permissionRequestLauncher,
                            sensorRequest = sensorRequest,
                            showMessage = showMessage,
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
    ) {
        val numTokens = tokensUsed + encoding.countTokens(list[list.size - 1].content)
        Log.i("ChatAPI", "Number of tokens: $numTokens")
        if (numTokens > maxTokens) {
            Log.i("ChatAPI", "Too many tokens, truncating messages")
            list.removeAt(0)
            tokensUsed -= encoding.countTokens(list[0].content)
            countTokensAndTruncate(list, encoding, maxTokens)
        }
    }

    fun setTimeoutSec(timeoutSec: Int) {
        if (timeoutSec != this.timeoutSec) {
            this.timeoutSec = timeoutSec
            client = defaultClient(apiKey, Duration.ofSeconds(timeoutSec.toLong()))
                .newBuilder()
                .build()
            retrofit = defaultRetrofit(client, mapper)

            api = retrofit.create(OpenAiApi::class.java)
            service = OpenAiService(api)
        }
    }
}

data class GPTModel(val name: String, val maxTokens: Int)

val availableModels = listOf(
    GPTModel("gpt-3.5-turbo", 4096),
    GPTModel("gpt-4", 4096)
)

