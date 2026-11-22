package uk.aviationdb.android.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiErrorBody(
    val error: ApiErrorDetail?,
)

@JsonClass(generateAdapter = true)
data class ApiErrorDetail(
    val code: Int,
    val message: String,
)

@JsonClass(generateAdapter = true)
data class ActiveFlightsResponse(
    @Json(name = "active_now") val activeNow: Int,
    @Json(name = "track_lat") val trackLat: Double? = null,
    @Json(name = "track_lon") val trackLon: Double? = null,
    @Json(name = "tracker_sec") val trackerSec: Int? = 20,
    val flights: List<ActiveFlight>,
)

@JsonClass(generateAdapter = true)
data class ActiveFlight(
    val callsign: String?,
    val icao24: String?,
    val registration: String?,
    val lat: Double?,
    val lon: Double?,
    val airline: String?,
    @Json(name = "origin_icao") val originIcao: String?,
    @Json(name = "dest_icao") val destIcao: String?,
    @Json(name = "aircraft_type") val aircraftType: String?,
    @Json(name = "model_icao") val modelIcao: String? = null,
    val media: FlightMedia? = null,
    val altitude: Int?,
    val speed: Int?,
    val heading: Int?,
    @Json(name = "last_updated") val lastUpdated: String?,
)

@JsonClass(generateAdapter = true)
data class SightingsResponse(
    val sightings: List<Sighting>?,
    val limit: Int?,
    val offset: Int?,
    val total: Int?,
)

@JsonClass(generateAdapter = true)
data class Sighting(
    val registration: String?,
    val callsign: String?,
    @Json(name = "seen_at") val seenAt: String?,
    val operator: String?,
    @Json(name = "aircraft_type") val aircraftType: String?,
    @Json(name = "route_from") val routeFrom: String?,
    @Json(name = "route_to") val routeTo: String?,
    @Json(name = "region_label") val regionLabel: String?,
)

@JsonClass(generateAdapter = true)
data class SummaryResponse(
    @Json(name = "active_now") val activeNow: Int?,
    @Json(name = "total_sightings") val totalSightings: Int?,
    @Json(name = "unique_registrations") val uniqueRegistrations: Int?,
    val plan: SummaryPlan?,
)

@JsonClass(generateAdapter = true)
data class SummaryPlan(
    val name: String?,
    @Json(name = "history_days") val historyDays: Int?,
)