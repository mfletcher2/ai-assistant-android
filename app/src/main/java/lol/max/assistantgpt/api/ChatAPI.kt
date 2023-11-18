package lol.max.assistantgpt.api

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.fasterxml.jackson.databind.ObjectMapper
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.FunctionExecutor
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.service.OpenAiService.defaultClient
import com.theokanning.openai.service.OpenAiService.defaultObjectMapper
import com.theokanning.openai.service.OpenAiService.defaultRetrofit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.lang.ref.WeakReference
import java.time.Duration


class ChatAPI(
    apiKey: String,
    timeoutSec: Long = 60,
    context: WeakReference<Context> = WeakReference(null)
) {


    private val functionExecutor = FunctionExecutor(FunctionCalling(context.get()).functionList)

    private var mapper: ObjectMapper = defaultObjectMapper()
    private var client: OkHttpClient = defaultClient(apiKey, Duration.ofSeconds(timeoutSec))
        .newBuilder()
        .build()
    private var retrofit: Retrofit = defaultRetrofit(client, mapper)

    private var api: OpenAiApi = retrofit.create(OpenAiApi::class.java)
    private var service: OpenAiService = OpenAiService(api)


    suspend fun getCompletion(
        chatMessages: List<ChatMessage>,
        model: String,
        snackbarHostState: SnackbarHostState? = null
    ): List<ChatMessage> {
        val newMessages: ArrayList<ChatMessage> = arrayListOf()

        val chatCompletionRequest = ChatCompletionRequest.builder()
            .messages(chatMessages)
            .model(model)
            .n(1)
            .temperature(0.5)
            .maxTokens(256)
            .functions(functionExecutor.functions)
            .build()
        try {
            val responseRequest = service.createChatCompletion(chatCompletionRequest).choices[0]
            val responseMessage = responseRequest.message
            Log.i("AssistantGPT", "GPT responded: ${responseMessage.content}")
            Log.i("AssistantGPT", "with stop reason: ${responseRequest.finishReason}")
            newMessages.add(responseMessage)

            val functionCall = responseMessage.functionCall
            if (functionCall != null) {
                snackbarHostState?.showSnackbar("${functionCall.name}(${functionCall.arguments})")
                Log.i(
                    "AssistantGPT",
                    "GPT is running this function: ${functionCall.name}(${functionCall.arguments})"
                )
                val functionResponseMessage =
                    functionExecutor.executeAndConvertToMessageHandlingExceptions(responseMessage.functionCall)
                newMessages.add(functionResponseMessage)
                chatCompletionRequest.messages.addAll(newMessages)
                newMessages.addAll(
                    getCompletion(
                        chatCompletionRequest.messages,
                        model,
                        snackbarHostState
                    )
                )
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
            newMessages.add(
                ChatMessage(
                    ChatMessageRole.ASSISTANT.value(),
                    "Sorry, an error occured.\n${e.message}"
                )
            )
        }
        return newMessages
    }
}

