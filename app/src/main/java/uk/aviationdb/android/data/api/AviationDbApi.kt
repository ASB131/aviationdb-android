package uk.aviationdb.android.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface AviationDbApi {
    @GET("me/active")
    suspend fun activeFlights(): ActiveFlightsResponse

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

    @GET("airlines")
    suspend fun airlineShort(
        @Query("airline_short") query: String,
    ): Map<String, Any?>
}