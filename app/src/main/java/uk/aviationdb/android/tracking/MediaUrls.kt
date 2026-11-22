package uk.aviationdb.android.tracking

import uk.aviationdb.android.BuildConfig
import uk.aviationdb.android.data.api.FlightMedia

object MediaUrls {
    fun absolute(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://", true) || path.startsWith("https://", true)) return path
        val base = BuildConfig.SITE_BASE_URL.trimEnd('/')
        return if (path.startsWith("/")) "$base$path" else "$base/$path"
    }

    fun listArtwork(media: FlightMedia?, dark: Boolean = false): String? {
        if (media == null) return null
        val order = if (dark) {
            listOf(media.livery, media.white, media.iconDark, media.icon)
        } else {
            listOf(media.livery, media.white, media.icon, media.iconDark)
        }
        return order.firstNotNullOfOrNull { absolute(it) }
    }
}