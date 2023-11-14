package lol.max.assistantgpt

import android.util.Log
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
import java.time.Duration


class ChatAPI(apiKey: String, timeoutSec: Long = 60) {
    private val functionExecutor = FunctionExecutor(listOf())
//    private val service = OpenAiService(apiKey)

    private var mapper: ObjectMapper = defaultObjectMapper()
    private var client: OkHttpClient = defaultClient(apiKey, Duration.ofSeconds(timeoutSec))
        .newBuilder()
        .build()
    private var retrofit: Retrofit = defaultRetrofit(client, mapper)

    private var api: OpenAiApi = retrofit.create(OpenAiApi::class.java)
    private var service: OpenAiService = OpenAiService(api)


    fun getCompletion(chatMessages: List<ChatMessage>): List<ChatMessage> {
        val newMessages: ArrayList<ChatMessage> = arrayListOf()

        val chatCompletionRequest = ChatCompletionRequest.builder()
            .messages(chatMessages)
            .model("gpt-3.5-turbo")
            .n(1)
            .maxTokens(128)
//            .functions(functionExecutor)
            .build()
        try {
            val responseMessage =
                service.createChatCompletion(chatCompletionRequest).choices[0].message
            Log.i("AssistantGPT", "GPT responded: ${responseMessage.content}")
            newMessages.add(responseMessage)

            val functionCall = responseMessage.functionCall
            if (functionCall != null) {
                val functionResponse =
                    functionExecutor.executeAndConvertToMessageHandlingExceptions(responseMessage.functionCall)
                newMessages.add(functionResponse)
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
            newMessages.add(
                ChatMessage(
                    ChatMessageRole.ASSISTANT.value(),
                    "Sorry, your request timed out."
                )
            )
        }
        return newMessages
    }
}

