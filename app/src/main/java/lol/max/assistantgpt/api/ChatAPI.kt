package lol.max.assistantgpt.api

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.FunctionExecutor
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.service.OpenAiService.defaultClient
import com.theokanning.openai.service.OpenAiService.defaultObjectMapper
import com.theokanning.openai.service.OpenAiService.defaultRetrofit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.time.Duration


class ChatAPI(
    apiKey: String,
    timeoutSec: Long = 60,
) {
    private var mapper: ObjectMapper = defaultObjectMapper()
    private var client: OkHttpClient = defaultClient(apiKey, Duration.ofSeconds(timeoutSec))
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
        context: Context,
        allowSensors: Boolean,
        showMessage: (String) -> Unit
    ): ArrayList<ChatMessage> {
        var messagesListCopy = ArrayList(chatMessages)

        countTokensAndTruncate(
            messagesListCopy,
            encodingRegistry.getEncodingForModel(model.name).get(),
            model.maxTokens
        )

        val functionExecutor = FunctionExecutor(Functions(context).getFunctionList(allowSensors))

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
                showMessage("${functionCall.name}${functionCall.arguments.toPrettyString()}")
                Log.i(
                    "AssistantGPT",
                    "GPT is running this function: ${functionCall.name}${functionCall.arguments}"
                )
                val functionResponseMessage =
                    functionExecutor.executeAndConvertToMessageHandlingExceptions(responseMessage.functionCall)
                Log.i("AssistantGPT", "Function response: ${functionResponseMessage.content}")
                messagesListCopy.add(functionResponseMessage)
                messagesListCopy = getCompletion(
                    messagesListCopy,
                    model,
                    context,
                    allowSensors,
                    showMessage
                )
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
            showMessage("Sorry, an error occured.\n${e.message}")
        }
        return messagesListCopy
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
}

data class GPTModel(val name: String, val maxTokens: Int)

val availableModels = listOf(
    GPTModel("gpt-3.5-turbo", 4096),
    GPTModel("gpt-3.5-turbo-16k", 16385),
    GPTModel("gpt-4", 8192)
)

