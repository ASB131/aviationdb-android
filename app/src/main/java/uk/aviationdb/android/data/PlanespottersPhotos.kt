package uk.aviationdb.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object PlanespottersPhotos {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun photoUrls(registration: String, max: Int = 12): List<String> = withContext(Dispatchers.IO) {
        val reg = registration.trim().uppercase()
        if (reg.length < 2) return@withContext emptyList()
        try {
            val req = Request.Builder()
                .url("https://api.planespotters.net/pub/photos/reg/$reg")
                .header("Accept", "application/json")
                .build()
            val body = client.newCall(req).execute().use { it.body?.string() } ?: return@withContext emptyList()
            val root = JSONObject(body)
            val photos = root.optJSONArray("photos") ?: return@withContext emptyList()
            val urls = mutableListOf<String>()
            for (i in 0 until photos.length()) {
                if (urls.size >= max) break
                val photo = photos.optJSONObject(i) ?: continue
                val thumb = photo.optJSONObject("thumbnail") ?: continue
                val thumbLarge = photo.optJSONObject("thumbnail_large")
                val url = thumbLarge?.optString("src")?.takeIf { it.isNotBlank() }
                    ?: thumb.optString("src").takeIf { it.isNotBlank() }
                if (url != null) urls += url
            }
            urls
        } catch (_: Exception) {
            emptyList()
        }
    }
}