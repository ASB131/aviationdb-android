package uk.aviationdb.android.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AviationDbApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout()

    @GET("auth/session")
    suspend fun session(): SessionResponse
    @GET("me/active")
    suspend fun activeFlights(): ActiveFlightsResponse

    @GET("me/profile")
    suspend fun profile(): ProfileResponse

    @GET("me/stats_overview")
    suspend fun statsOverview(): StatsOverviewResponse

    @GET("me/aircraft")
    suspend fun meAircraft(
        @Query("registration") registration: String,
        @Query("limit") limit: Int = 10,
    ): Map<String, Any?>

    @GET("me/sightings")
    suspend fun sightings(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): SightingsResponse

    @GET("me/summary")
    suspend fun summary(): SummaryResponse

    @GET("aircraft")
    suspend fun aircraftShort(
        @Query("aircraft_short") registration: String,
    ): Map<String, Any?>

    @GET("aircraft")
    suspend fun aircraftFull(
        @Query("aircraft_full") registration: String,
    ): Map<String, Any?>

    @GET("airlines")
    suspend fun airlineShort(
        @Query("airline_short") query: String,
    ): Map<String, Any?>

    @GET("airports")
    suspend fun airportLookup(
        @Query("airport_short") ident: String,
    ): Map<String, Any?>

    @GET("callsigns")
    suspend fun callsignLookup(
        @Query("callsign_search") callsign: String,
    ): Map<String, Any?>
}