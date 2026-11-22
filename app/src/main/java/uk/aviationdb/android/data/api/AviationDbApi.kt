package uk.aviationdb.android.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    @GET("me/stats_summary")
    suspend fun statsSummary(@Query("range") range: String? = null): StatsSummaryResponse

    @GET("me/stats_kpis")
    suspend fun statsKpis(@Query("range") range: String? = null): StatsKpisResponse

    @GET("me/stats_charts")
    suspend fun statsCharts(
        @Query("metric") metric: String,
        @Query("range") range: String? = null,
    ): StatsChartResponse

    @GET("me/tracking")
    suspend fun getTracking(): TrackingResponse

    @PATCH("me/tracking")
    suspend fun updateTracking(@Body body: TrackingUpdateRequest): TrackingResponse

    @POST("me/billing_portal")
    suspend fun billingPortal(): BillingPortalResponse

    @GET("me/aircraft")
    suspend fun meAircraft(
        @Query("registration") registration: String,
        @Query("limit") limit: Int = 20,
    ): MeAircraftResponse

    @GET("me/sightings")
    suspend fun sightings(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): SightingsResponse

    @GET("me/summary")
    suspend fun summary(): SummaryResponse

    @GET("aircraft")
    suspend fun aircraftSearch(
        @Query("aircraft_search") query: String,
        @Query("limit") limit: Int = 25,
        @Query("offset") offset: Int = 0,
    ): AircraftSearchResponse

    @GET("airlines")
    suspend fun airlineSearch(
        @Query("airline_search") query: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): AirlineSearchResponse

    @GET("aircraft")
    suspend fun aircraftShort(
        @Query("aircraft_short") registration: String,
    ): AircraftShortResponse

    @GET("aircraft")
    suspend fun aircraftFull(
        @Query("aircraft_full") registration: String,
    ): Map<String, Any?>

    @GET("airlines")
    suspend fun airlineShort(
        @Query("airline_short") query: String,
    ): AirlineShortResponse

    @GET("airlines")
    suspend fun airlineFull(
        @Query("airline_full") query: String,
    ): Map<String, Any?>

    @GET("airports")
    suspend fun airportFull(
        @Query("airport_full") ident: String,
    ): Map<String, Any?>

    @GET("airports")
    suspend fun airportLookup(
        @Query("airport_short") ident: String,
    ): AirportShortResponse

    @GET("callsigns")
    suspend fun callsignLookup(
        @Query("callsign_search") callsign: String,
    ): CallsignSearchResponse
}