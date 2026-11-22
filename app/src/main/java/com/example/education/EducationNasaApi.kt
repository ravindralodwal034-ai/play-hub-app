package com.example.education

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SpaceLaunchItem(
    val id: String,
    val name: String,
    val provider: String,
    val statusName: String,
    val netDate: String,
    val padLocation: String,
    val missionName: String,
    val missionDescription: String,
    val orbit: String,
    val isUpcoming: Boolean
)

data class NasaApodItem(
    val title: String,
    val date: String,
    val explanation: String,
    val url: String,
    val mediaType: String
)

data class ScientistSummary(
    val title: String,
    val description: String,
    val extract: String,
    val pageUrl: String
)

interface EducationNasaApiService {
    @GET("https://ll.thespacedevs.com/2.2.0/launch/upcoming/")
    suspend fun getUpcomingLaunches(@Query("limit") limit: Int = 12): ResponseBody

    @GET("https://ll.thespacedevs.com/2.2.0/launch/previous/")
    suspend fun getPreviousLaunches(@Query("limit") limit: Int = 12): ResponseBody

    @GET("https://api.nasa.gov/planetary/apod")
    suspend fun getNasaApod(@Query("api_key") apiKey: String = "DEMO_KEY"): ResponseBody

    @GET
    suspend fun getWikipediaSummary(@Url fullUrl: String): ResponseBody
}

class EducationNasaRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "TubeStoreAndroid/1.0 (Educational Space Viewer)")
                .build()
            chain.proceed(request)
        }
        .build()

    private val api: EducationNasaApiService = Retrofit.Builder()
        .baseUrl("https://en.wikipedia.org/")
        .client(client)
        .build()
        .create(EducationNasaApiService::class.java)

    suspend fun fetchLaunches(): Result<List<SpaceLaunchItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val upcomingJson = JSONObject(api.getUpcomingLaunches(10).string())
            val previousJson = JSONObject(api.getPreviousLaunches(10).string())
            val combined = mutableListOf<SpaceLaunchItem>()
            combined.addAll(parseLaunchesArray(upcomingJson, isUpcoming = true))
            combined.addAll(parseLaunchesArray(previousJson, isUpcoming = false))
            combined
        }
    }

    suspend fun fetchNasaApod(): Result<NasaApodItem> = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject(api.getNasaApod().string())
            NasaApodItem(
                title = json.optString("title", "NASA Astronomy Picture of the Day"),
                date = json.optString("date", ""),
                explanation = json.optString("explanation", ""),
                url = json.optString("url", "https://apod.nasa.gov"),
                mediaType = json.optString("media_type", "image")
            )
        }
    }

    suspend fun fetchScientistInfo(query: String): Result<ScientistSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val formatted = query.trim().replace(" ", "_")
            val encoded = URLEncoder.encode(formatted, "UTF-8").replace("+", "_")
            val raw = api.getWikipediaSummary("https://en.wikipedia.org/api/rest_v1/page/summary/$encoded").string()
            val json = JSONObject(raw)
            val contentUrls = json.optJSONObject("content_urls")?.optJSONObject("desktop")
            ScientistSummary(
                title = json.optString("title", query),
                description = json.optString("description", "Scientist & Researcher"),
                extract = json.optString("extract", "No summary available."),
                pageUrl = contentUrls?.optString("page") ?: "https://en.wikipedia.org/wiki/$encoded"
            )
        }
    }

    private fun parseLaunchesArray(root: JSONObject, isUpcoming: Boolean): List<SpaceLaunchItem> {
        val arr = root.optJSONArray("results") ?: return emptyList()
        val list = ArrayList<SpaceLaunchItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val provider = obj.optJSONObject("launch_service_provider")?.optString("name", "Space Agency")
                ?: "Space Agency"
            val statusName = obj.optJSONObject("status")?.optString("name", if (isUpcoming) "Scheduled" else "Completed")
                ?: "Scheduled"
            val padObj = obj.optJSONObject("pad")
            val padLocation = padObj?.optJSONObject("location")?.optString("name")
                ?: padObj?.optString("name")
                ?: "Launch Complex"
            val missionObj = obj.optJSONObject("mission")
            val missionName = missionObj?.optString("name")?.takeIf { it.isNotBlank() } ?: "Orbital Mission"
            val missionDesc = missionObj?.optString("description")?.takeIf { it.isNotBlank() }
                ?: "Official launch telemetry and payload deployment mission."
            val orbit = missionObj?.optJSONObject("orbit")?.optString("name") ?: "Orbit"
            list.add(
                SpaceLaunchItem(
                    id = obj.optString("id", "launch_${isUpcoming}_$i"),
                    name = obj.optString("name", "Space Mission"),
                    provider = provider,
                    statusName = statusName,
                    netDate = obj.optString("net", "").take(16).replace("T", " "),
                    padLocation = padLocation,
                    missionName = missionName,
                    missionDescription = missionDesc,
                    orbit = orbit,
                    isUpcoming = isUpcoming
                )
            )
        }
        return list
    }
}
