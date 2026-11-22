package uk.aviationdb.android.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FlightMedia(
    @Json(name = "airline_icao") val airlineIcao: String? = null,
    val icon: String? = null,
    @Json(name = "icon_dark") val iconDark: String? = null,
    val banner: String? = null,
    @Json(name = "banner_transparent") val bannerTransparent: String? = null,
    val livery: String? = null,
    val white: String? = null,
    @Json(name = "brand_accent") val brandAccent: String? = null,
)
