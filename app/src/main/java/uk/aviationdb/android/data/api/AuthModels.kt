package uk.aviationdb.android.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
    val remember: Boolean = true,
    @Json(name = "app_version") val appVersion: String,
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_at") val expiresAt: String?,
    @Json(name = "token_type") val tokenType: String?,
    val user: AuthUser,
)

@JsonClass(generateAdapter = true)
data class AuthUser(
    val id: Int?,
    val email: String,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "has_tracking") val hasTracking: Boolean?,
    @Json(name = "plan_name") val planName: String?,
    @Json(name = "track_lat") val trackLat: Double?,
    @Json(name = "track_lon") val trackLon: Double?,
)

@JsonClass(generateAdapter = true)
data class SessionResponse(
    val user: AuthUser,
    @Json(name = "auth_via") val authVia: String?,
)

@JsonClass(generateAdapter = true)
data class ApiErrorEnvelope(
    val error: ApiErrorDetail?,
)