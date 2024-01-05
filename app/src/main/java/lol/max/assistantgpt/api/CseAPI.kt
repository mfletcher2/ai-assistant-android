package lol.max.assistantgpt.api

import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import lol.max.assistantgpt.BuildConfig
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class CseAPI {
    @JsonPropertyDescription("The query to search")
    @JsonProperty(required = true)
    lateinit var query: String

    @JsonIgnore
    private val pseId: String = BuildConfig.GOOGLE_SEARCH_ID

    @JsonIgnore
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://customsearch.googleapis.com/").build()

    @JsonIgnore
    private val googleCseService = retrofit.create(GoogleCseService::class.java)

    fun doSearch(query: String, key: String): List<Result> {
        val response = googleCseService.getSearch(
            key,
            pseId,
            query
        )
        Log.i("CseAPI", "Performing search for \"$query\"")

        return try {
            val search = response.execute().body()
            search?.items ?: listOf()
        } catch (e: Exception) {
            listOf(Result("An error occurred.\n${e.message}", "", ""))
        }
    }
}


interface GoogleCseService {
    @GET("customsearch/v1")
    fun getSearch(
        @Query("key") key: String,
        @Query("cx") id: String,
        @Query("q") query: String,
        @Query("num") num: Int = 10,
        @Query("fields") fields: String = "items(title, displayLink, snippet)"
    ): Call<Search>
}

data class Search(
    val items: List<Result>
)

data class Result(
    val title: String,
    val displayLink: String,
    val snippet: String
)
