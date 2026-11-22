package uk.aviationdb.android.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AircraftSearchResponse(
    val query: String?,
    val total: Int?,
    val results: List<AircraftBrowse>?,
)

@JsonClass(generateAdapter = true)
data class AircraftShortResponse(
    val aircraft: AircraftBrowse?,
)

@JsonClass(generateAdapter = true)
data class AircraftBrowse(
    val registration: String?,
    val icao24: String?,
    @Json(name = "icao_type") val icaoType: String?,
    @Json(name = "aircraft_type") val aircraftType: String?,
    @Json(name = "aircraft_model") val aircraftModel: String?,
    val manufacturer: String?,
    val model: String?,
    val operator: String?,
    @Json(name = "airline_code") val airlineCode: String?,
    @Json(name = "is_private_operator") val isPrivateOperator: Boolean?,
)

@JsonClass(generateAdapter = true)
data class AirlineShortResponse(
    val airline: AirlineBrowse?,
)

@JsonClass(generateAdapter = true)
data class AirlineBrowse(
    val icao: String?,
    val iata: String?,
    val name: String?,
    val country: String?,
    val region: String?,
    val status: String?,
)

@JsonClass(generateAdapter = true)
data class AirportShortResponse(
    val airport: AirportBrowse?,
)

@JsonClass(generateAdapter = true)
data class AirportBrowse(
    val ident: String?,
    val name: String?,
    @Json(name = "iso_country") val isoCountry: String?,
    @Json(name = "gps_code") val gpsCode: String?,
    val iata: String?,
    val FIR: String?,
    val municipality: String?,
    val coordinates: String?,
    @Json(name = "elevation_ft") val elevationFt: Int?,
)

@JsonClass(generateAdapter = true)
data class CallsignSearchResponse(
    val master: CallsignMaster?,
    val flights: List<CallsignFlightRow>?,
)

@JsonClass(generateAdapter = true)
data class CallsignMaster(
    val callsign: String?,
    val operator: String?,
    @Json(name = "route_from") val routeFrom: String?,
    @Json(name = "route_to") val routeTo: String?,
    @Json(name = "callsigns_total") val callsignsTotal: Int?,
    @Json(name = "last_seen_time") val lastSeenTime: String?,
)

@JsonClass(generateAdapter = true)
data class CallsignFlightRow(
    val registration: String?,
    @Json(name = "route_from") val routeFrom: String?,
    @Json(name = "route_to") val routeTo: String?,
    val operator: String?,
    @Json(name = "aircraft_type") val aircraftType: String?,
)