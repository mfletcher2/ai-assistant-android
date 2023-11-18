package lol.max.assistantgpt.api

import android.content.Context
import android.util.Log
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.messages.Message
import com.theokanning.openai.messages.MessageRequest
import com.theokanning.openai.runs.RunCreateRequest
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.threads.ThreadRequest
import lol.max.assistantgpt.BuildConfig
import java.lang.Thread.sleep
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class AssistantsAPI(context: Context, apiKey: String, var threadIds: List<String>, currentThread: Int = 0, private val saveThreads: () -> Unit) {
    private val contextRef = WeakReference(context)
    private var service: OpenAiService = OpenAiService(apiKey)

    init {
        thread {
            if(threadIds.isEmpty()) {
                threadIds = listOf(service.createThread(ThreadRequest()).id)
            }
            saveThreads()
        }
    }

    var currentThread = currentThread
        set(value) {
            field = value
            saveThreads()
        }

    fun getMessagesFromAPI(): List<Message>? {
        return try {
            service.listMessages(threadIds[currentThread]).getData()
        } catch (e: Exception) {
            null
        }
    }

    fun getResponse(str: String, model: String, allowSensors: Boolean, showInfo: (String) -> Unit): List<Message>? {
        try {
            val messageRequest = MessageRequest(ChatMessageRole.USER.value(), str, null, null)
            service.createMessage(threadIds[currentThread], messageRequest)
            var run = service.createRun(
                threadIds[currentThread],
                RunCreateRequest(BuildConfig.ASSISTANT_ID, model, null, FunctionCalling(contextRef.get()).getFunctionToolsList(allowSensors), null)
            )
            do {
                sleep(500)
                run = service.retrieveRun(threadIds[currentThread], run.id)
                Log.i("Assistants API", "status: ${run.status}")
            } while (run.status == "queued" || run.status == "in_progress")
            return getMessagesFromAPI()
        } catch (e: Exception) {
            showInfo("An error occurred: ${e.message.toString()})")
            e.printStackTrace()
            return getMessagesFromAPI()
        }
    }
}