package lol.max.assistantgpt.api

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.theokanning.openai.completion.chat.ChatFunction
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

val weatherChatFunction = ChatFunction.builder()
    .name("get_weather")
    .description("Get the weather forecast for a location in the United States.")
    .executor(WeatherAPI::class.java) { it.getWeather(it.address) }
    .build()

class WeatherAPI {
    @JsonPropertyDescription("The location to get the weather for. It must at least specify the city and state. Example: New York, NY")
    @JsonProperty(required = true)
    lateinit var address: String
    private val retrofitGoogle = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://maps.googleapis.com/maps/api/").build()
    private val googleGeocodeService = retrofitGoogle.create(GoogleGeocodeService::class.java)

    private val retrofitNWS = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://api.weather.gov/").build()
    private val nwsApiService = retrofitNWS.create(NWSAPIService::class.java)

    fun getWeather(address: String): List<Period> {
        val latLonRequest = googleGeocodeService.getLatLon(address).request()
        Log.i("WeatherAPI", "Doing geocode request: ${latLonRequest.url()}")
        val latLonResponse = googleGeocodeService.getLatLon(address).execute()

        Log.i(
            "WeatherAPI",
            "Geocode service status code: ${latLonResponse.code()}: ${latLonResponse.message()}"
        )
        if (latLonResponse.body()!!.status != "OK")
            throw Exception("Geocode service error: ${latLonResponse.body()!!.status}")

        val latitude = latLonResponse.body()!!.results[0].geometry.location.lat
        val longitude = latLonResponse.body()!!.results[0].geometry.location.lng

        val stationRequest = nwsApiService.getStationInfo(latitude, longitude).request()
        Log.i("WeatherAPI", "Doing station request: ${stationRequest.url()}")
        val stationResponse = nwsApiService.getStationInfo(latitude, longitude).execute()
        Log.i(
            "WeatherAPI",
            "Station service status code: ${stationResponse.code()}: ${stationResponse.message()}"
        )
        val office = stationResponse.body()!!.properties.cwa
        val gridX = stationResponse.body()!!.properties.gridX
        val gridY = stationResponse.body()!!.properties.gridY

        val weatherRequest = nwsApiService.getForecast(office, gridX, gridY).request()
        Log.i("WeatherAPI", "Doing weather request: ${weatherRequest.url()}")
        val forecastResponse = nwsApiService.getForecast(office, gridX, gridY).execute()
        Log.i(
            "WeatherAPI",
            "Weather service status code: ${forecastResponse.code()}: ${forecastResponse.message()}"
        )
        return forecastResponse.body()!!.properties.periods
    }
}

interface NWSAPIService {
    @GET("points/{latitude},{longitude}")
    fun getStationInfo(
        @Path("latitude") latitude: Float,
        @Path("longitude") longitude: Float
    ): Call<StationResult>

    @GET("gridpoints/{office}/{gridX},{gridY}/forecast")
    fun getForecast(
        @Path("office") officeId: String,
        @Path("gridX") gridX: Int,
        @Path("gridY") gridY: Int
    ): Call<StationResult>
}

data class StationResult(val properties: Properties)
data class Properties(val gridX: Int, val gridY: Int, val cwa: String, val periods: List<Period>)
data class Period(
    val name: String,
    val temperature: Int,
    val temperatureUnit: String,
    val detailedForecast: String,
    val startTime: String
)

interface GoogleGeocodeService {
    @GET("geocode/json")
    fun getLatLon(
        @Query("address") address: String,
        @Query("key") key: String = "AIzaSyDKMdWHQqBxRR_KUnikaEjTOuATHhCaTIU"
    ): Call<Results>
}

data class Results(val results: List<LatLonResult>, val status: String)
data class LatLonResult(val geometry: Geometry)
data class Geometry(val location: Location)
data class Location(val lat: Float, val lng: Float)