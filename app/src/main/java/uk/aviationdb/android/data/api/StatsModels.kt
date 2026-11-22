package uk.aviationdb.android.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatsSummaryResponse(
    val range: String?,
    @Json(name = "history_days") val historyDays: Int?,
    @Json(name = "leading_operator") val leadingOperator: StatsLeader?,
    @Json(name = "leading_aircraft") val leadingAircraft: StatsLeader?,
    val current: StatsSummaryCurrent?,
    val change: StatsSummaryChange?,
)

@JsonClass(generateAdapter = true)
data class StatsLeader(
    val label: String?,
    val registration: String?,
    val operator: String?,
    val c: Int?,
    val media: Map<String, Any?>? = null,
)

@JsonClass(generateAdapter = true)
data class StatsSummaryCurrent(
    @Json(name = "total_flights") val totalFlights: Int?,
    @Json(name = "total_aircraft") val totalAircraft: Int?,
    @Json(name = "total_callsigns") val totalCallsigns: Int?,
    @Json(name = "busiest_day") val busiestDay: String?,
    @Json(name = "busiest_day_count") val busiestDayCount: Int?,
    @Json(name = "new_aircraft") val newAircraft: Int?,
    @Json(name = "longest_streak") val longestStreak: Int?,
    @Json(name = "current_streak") val currentStreak: Int?,
)

@JsonClass(generateAdapter = true)
data class StatsSummaryChange(
    @Json(name = "total_flights") val totalFlights: Double?,
    @Json(name = "total_aircraft") val totalAircraft: Double?,
    @Json(name = "total_callsigns") val totalCallsigns: Double?,
)

@JsonClass(generateAdapter = true)
data class StatsChartResponse(
    val range: String?,
    val metric: String?,
    val bucket: String?,
    val title: String?,
    val rows: List<StatsChartRow>?,
)

@JsonClass(generateAdapter = true)
data class StatsKpisResponse(
    val range: String?,
    @Json(name = "history_days") val historyDays: Int?,
    @Json(name = "total_flights") val totalFlights: Int?,
    @Json(name = "active_days") val activeDays: Int?,
    @Json(name = "avg_per_day") val avgPerDay: Double?,
    @Json(name = "avg_per_hour") val avgPerHour: Double?,
    @Json(name = "with_route") val withRoute: Int?,
    @Json(name = "route_pct") val routePct: Double?,
    @Json(name = "unique_airports") val uniqueAirports: Int?,
    @Json(name = "single_visit_registrations") val singleVisitRegistrations: Int?,
    @Json(name = "peak_hour") val peakHour: Int?,
    @Json(name = "peak_hour_count") val peakHourCount: Int?,
)

@JsonClass(generateAdapter = true)
data class StatsChartRow(
    val label: String?,
    val operator: String?,
    val registration: String?,
    val c: Int?,
    val t: String?,
    val media: Map<String, Any?>? = null,
)

@JsonClass(generateAdapter = true)
data class TrackingResponse(
    @Json(name = "track_lat") val trackLat: Double?,
    @Json(name = "track_lon") val trackLon: Double?,
    val timezone: String?,
    val ok: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class TrackingUpdateRequest(
    @Json(name = "track_lat") val trackLat: Double?,
    @Json(name = "track_lon") val trackLon: Double?,
    val timezone: String? = null,
)

@JsonClass(generateAdapter = true)
data class BillingPortalResponse(
    val url: String?,
)

@JsonClass(generateAdapter = true)
data class MeAircraftResponse(
    val registration: String?,
    @Json(name = "history_days") val historyDays: Int?,
    val aircraft: AircraftBrowse?,
    @Json(name = "aircraft_full") val aircraftFull: Map<String, Any?>?,
    @Json(name = "your_stats") val yourStats: MeAircraftStats?,
    val routes: List<MeAircraftRoute>?,
    val sightings: List<MeAircraftSighting>?,
)

@JsonClass(generateAdapter = true)
data class MeAircraftStats(
    @Json(name = "total_sightings") val totalSightings: Int?,
    @Json(name = "unique_callsigns") val uniqueCallsigns: Int?,
    @Json(name = "first_seen") val firstSeen: String?,
    @Json(name = "last_seen") val lastSeen: String?,
)

@JsonClass(generateAdapter = true)
data class MeAircraftRoute(
    @Json(name = "route_from") val routeFrom: String?,
    @Json(name = "route_to") val routeTo: String?,
    @Json(name = "times_seen") val timesSeen: Int?,
    @Json(name = "last_seen") val lastSeen: String?,
)

@JsonClass(generateAdapter = true)
data class MeAircraftSighting(
    val callsign: String?,
    @Json(name = "seen_at") val seenAt: String?,
    val operator: String?,
    @Json(name = "route_from") val routeFrom: String?,
    @Json(name = "route_to") val routeTo: String?,
)