package lol.max.assistantgpt

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.databind.ObjectMapper
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatFunction
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
    private val getAppsListFunction = ChatFunction.builder()
        .name("get_apps_list")
        .description("Get a list of installed applications.")
        .executor(PackageListRequest::class.java) { it.getPackages(context.get()) }
        .build()
    private val launchPackageFunction = ChatFunction.builder()
        .name("launch_package")
        .description("Launches the given package. Always confirm the package name first with get_apps_list(). Returns whether or not it was successful.")
        .executor(PackageRunRequest::class.java) { it.runPackage(context.get()) }
        .build()

    private val functionList = listOf(cseChatFunction, getAppsListFunction, launchPackageFunction)
    private val functionExecutor = FunctionExecutor(functionList)

    private var mapper: ObjectMapper = defaultObjectMapper()
    private var client: OkHttpClient = defaultClient(apiKey, Duration.ofSeconds(timeoutSec))
        .newBuilder()
        .build()
    private var retrofit: Retrofit = defaultRetrofit(client, mapper)

    private var api: OpenAiApi = retrofit.create(OpenAiApi::class.java)
    private var service: OpenAiService = OpenAiService(api)


    suspend fun getCompletion(
        chatMessages: List<ChatMessage>,
        model: String = "gpt-3.5-turbo",
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
            Log.i("AssistantGPT", "with response reason: ${responseRequest.finishReason}")
            newMessages.add(responseMessage)

            val functionCall = responseMessage.functionCall
            if (functionCall != null) {
                snackbarHostState?.showSnackbar("${functionCall.name}(${functionCall.arguments})")
                Log.i("AssistantGPT", "GPT is running this function: ${functionCall.name}(${functionCall.arguments})")
                val functionResponseMessage =
                    functionExecutor.executeAndConvertToMessageHandlingExceptions(responseMessage.functionCall)
                newMessages.add(functionResponseMessage)
                chatCompletionRequest.messages.addAll(newMessages)
                newMessages.addAll(getCompletion(chatCompletionRequest.messages, model, snackbarHostState))
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

class PackageListRequest {
    fun getPackages(context: Context?): List<PackageResult> {
        if (context == null)
            return listOf()

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pkgAppsList: List<ResolveInfo> =
            context.packageManager.queryIntentActivities(mainIntent, 0)

        val resultList = ArrayList<PackageResult>()
        pkgAppsList.forEach {
            resultList.add(
                PackageResult(
                    it.loadLabel(context.packageManager).toString(),
                    it.activityInfo.packageName
                )
            )
        }
        return resultList
    }
}

data class PackageResult(val appName: String, val packageName: String)

class PackageRunRequest {
    @JsonPropertyDescription("Name of the package to run")
    @JsonProperty(required = true)
    lateinit var packageName: String
    fun runPackage(context: Context?): Boolean {
        if (context == null) return false
        val i = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        i.addCategory(Intent.CATEGORY_LAUNCHER)
        context.startActivity(i)
        return true
    }
}
