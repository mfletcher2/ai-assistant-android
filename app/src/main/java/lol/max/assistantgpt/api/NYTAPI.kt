package lol.max.assistantgpt.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.theokanning.openai.completion.chat.ChatFunction
import lol.max.assistantgpt.BuildConfig
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import kotlin.math.min

val nytTopStoriesFunction = ChatFunction.builder()
    .name("get_top_stories")
    .description("Get the top stories from the New York Times.")
    .executor(NYTTopStoriesAPI::class.java) { it.getTopStories(it.category) }
    .build()
val nytArticleSearchFunction = ChatFunction.builder()
    .name("get_article_search")
    .description("Search the New York Times for a given query.")
    .executor(NYTArticleSearchAPI::class.java) { it.getArticleSearch(it.query) }
    .build()

class NYTTopStoriesAPI {
    @JsonPropertyDescription("The category of stories to find. Available values are arts, automobiles, books/review, business, fashion, food, health, home, insider, magazine, movies, nyregion, obituaries, opinion, politics, realestate, science, sports, sundayreview, technology, theater, t-magazine, travel, upshot, us, and world. Defaults to home.")
    var category: String = "home"

    @JsonIgnore
    private var apiKey: String = BuildConfig.NYT_API_KEY

    @JsonIgnore
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://api.nytimes.com/").build()

    @JsonIgnore
    private val nytApiService = retrofit.create(NYTAPIService::class.java)

    fun getTopStories(category: String = "home"): List<TopStoryResult> {
        val request = nytApiService.getTopStories(category, apiKey).execute()
        return if (request.body() != null && request.body()!!.status == "OK")
            request.body()!!.results.subList(0, min(10, request.body()!!.num_results))
        else return listOf()
    }
}

class NYTArticleSearchAPI {
    @JsonPropertyDescription("The query to search")
    @JsonProperty(required = true)
    lateinit var query: String

    @JsonIgnore
    private var apiKey: String = BuildConfig.NYT_API_KEY

    @JsonIgnore
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://api.nytimes.com/").build()

    @JsonIgnore
    private val nytApiService = retrofit.create(NYTAPIService::class.java)

    fun getArticleSearch(query: String): List<Article> {
        val request = nytApiService.getArticleSearch(query, apiKey).execute()
        return if (request.body() != null && request.body()!!.status == "OK") request.body()!!.response.docs
        else listOf()
    }
}

interface NYTAPIService {
    @GET("svc/topstories/v2/{category}.json")
    fun getTopStories(@Path("category") category: String, @Query("api-key") apiKey: String): Call<TopStoriesResponse>

    @GET("svc/search/v2/articlesearch.json")
    fun getArticleSearch(@Query("q") query: String, @Query("api-key") apiKey: String): Call<ArticleSearchResponse>
}

data class TopStoriesResponse(
    val status: String,
    val section: String,
    val last_updated: String,
    val num_results: Int,
    val results: List<TopStoryResult>
)

data class TopStoryResult(
    val section: String,
    val subsection: String,
    val title: String,
    val abstract: String,
    val url: String,
    val published_date: String
)

data class ArticleSearchResponse(val status: String, val response: ArticleResponse)
data class ArticleResponse(val docs: List<Article>)
data class Article(
    val headline: Headline,
    val byline: ByLine,
    val pub_date: String,
    val web_url: String,
    val abstract: String,
    val lead_paragraph: String
)

data class Headline(val main: String)
data class ByLine(val original: String)