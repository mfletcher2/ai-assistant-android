package lol.max.assistantgpt

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.theokanning.openai.completion.chat.ChatFunction
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

val cseChatFunction: ChatFunction = ChatFunction.builder()
    .name("get_web_search")
    .description("Search the web for a given query. You should use this if you are unsure of " +
            "the answer or need to fact-check something. Always cite the displayLink that you " +
            "used information from in your response.")
    .executor(CseAPI::class.java) { it.doSearch(it.query) }
    .build()

class CseAPI {
    @JsonPropertyDescription("The query to search")
    @JsonProperty(required = true)
    lateinit var query: String
    private val key: String = BuildConfig.GOOGLE_API_KEY
    private val pseId: String = BuildConfig.GOOGLE_SEARCH_ID
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://customsearch.googleapis.com/").build()
    private val googleCseService = retrofit.create(GoogleCseService::class.java)

    fun doSearch(query: String): List<Result> {
        val response = googleCseService.getSearch(
            key,
            pseId,
            query
        )
        Log.i("CseAPI", "Performing search for \"$query\"")

        val search = response.execute().body()
        return search?.items ?: listOf()
    }
}


interface GoogleCseService {
    @GET("customsearch/v1")
    fun getSearch(
        @Query("key") key: String,
        @Query("cx") id: String,
        @Query("q") query: String,
        @Query("num") num: Int = 10
    ): Call<Search>
}

data class Search(
    val queries: Queries, val items: List<Result>
)

data class Queries(
    val request: List<Request>
)

data class Request(
    val title: String
)

data class Result(
    val title: String,
    val displayLink: String,
    val snippet: String
)
