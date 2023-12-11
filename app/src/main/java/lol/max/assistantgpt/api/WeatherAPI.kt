package lol.max.assistantgpt.api

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import lol.max.assistantgpt.api.chat.LateResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import kotlin.concurrent.thread

class WeatherAPI {
    @JsonPropertyDescription("The address to get the weather for. It must at least specify the city and state. Example: New York, NY")
    @JsonProperty(required = true)
    lateinit var address: String

    @JsonIgnore
    private val retrofitGoogle = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://maps.googleapis.com/maps/api/").build()

    @JsonIgnore
    private val googleGeocodeService = retrofitGoogle.create(GoogleGeocodeService::class.java)

    fun getWeather(address: String): List<Period> {
        val latLonRequest = googleGeocodeService.getLatLon(address).request()
        Log.i("WeatherAPI", "Doing geocode request: ${latLonRequest.url}")
        val latLonResponse = googleGeocodeService.getLatLon(address).execute()

        Log.i(
            "WeatherAPI",
            "Geocode service status code: ${latLonResponse.code()}: ${latLonResponse.message()}"
        )
        if (latLonResponse.body()!!.status != "OK") {
            Log.e("WeatherAPI", "Geocode service error: ${latLonResponse.body()!!.status}")
            return listOf(Period("Error", 0, "", "Error", ""))
        }

        val latitude = latLonResponse.body()!!.results[0].geometry.location.lat
        val longitude = latLonResponse.body()!!.results[0].geometry.location.lng

        return WeatherByLatLonRequest().getWeather(latitude, longitude)
    }
}

class WeatherByLatLonRequest : LateResponse() {
    @SuppressLint("MissingPermission")

    @JsonIgnore
    private val retrofitNWS = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://api.weather.gov/").build()

    @JsonIgnore
    private val nwsApiService = retrofitNWS.create(NWSAPIService::class.java)

    @SuppressLint("MissingPermission")
    fun getWeatherFromLocation(context: Context?) {
        if (context == null)
            onSuccess("An error occurred.")
        LocationServices.getFusedLocationProviderClient(context!!)
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener {
                thread {
                    onSuccess(getWeather(it.latitude.toFloat(), it.longitude.toFloat()))
                }
            }
    }

    fun getWeather(latitude: Float, longitude: Float): List<Period> {
        val stationRequest = nwsApiService.getStationInfo(latitude, longitude).request()
        Log.i("WeatherAPI", "Doing station request: ${stationRequest.url}")
        val stationResponse = nwsApiService.getStationInfo(latitude, longitude).execute()
        Log.i(
            "WeatherAPI",
            "Station service status code: ${stationResponse.code()}: ${stationResponse.message()}"
        )
        val office = stationResponse.body()!!.properties.cwa
        val gridX = stationResponse.body()!!.properties.gridX
        val gridY = stationResponse.body()!!.properties.gridY

        val weatherRequest = nwsApiService.getForecast(office, gridX, gridY).request()
        Log.i("WeatherAPI", "Doing weather request: ${weatherRequest.url}")
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