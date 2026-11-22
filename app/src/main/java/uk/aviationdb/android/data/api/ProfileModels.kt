package uk.aviationdb.android.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProfileResponse(
    val user: ProfileUser,
    val plan: ProfilePlan,
    val billing: ProfileBilling,
)

@JsonClass(generateAdapter = true)
data class ProfileUser(
    val email: String,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "track_lat") val trackLat: Double?,
    @Json(name = "track_lon") val trackLon: Double?,
    val timezone: String?,
    @Json(name = "member_since") val memberSince: String?,
)

@JsonClass(generateAdapter = true)
data class ProfilePlan(
    val name: String?,
    @Json(name = "has_tracking") val hasTracking: Boolean?,
    @Json(name = "history_days") val historyDays: Int?,
    @Json(name = "price_label") val priceLabel: String?,
)

@JsonClass(generateAdapter = true)
data class ProfileBilling(
    @Json(name = "stripe_status") val stripeStatus: String?,
    @Json(name = "current_period_end") val currentPeriodEnd: String?,
    @Json(name = "recent_invoices") val recentInvoices: List<ProfileInvoice>?,
)

@JsonClass(generateAdapter = true)
data class ProfileInvoice(
    @Json(name = "amount_paid") val amountPaid: Int?,
    val currency: String?,
    val status: String?,
    @Json(name = "created_at") val createdAt: String?,
)

@JsonClass(generateAdapter = true)
data class StatsOverviewResponse(
    @Json(name = "history_days") val historyDays: Int?,
    @Json(name = "plan_name") val planName: String?,
    @Json(name = "active_now") val activeNow: Int?,
    @Json(name = "total_sightings") val totalSightings: Int?,
    @Json(name = "unique_registrations") val uniqueRegistrations: Int?,
    @Json(name = "unique_callsigns") val uniqueCallsigns: Int?,
    @Json(name = "top_registrations") val topRegistrations: List<TopCount>?,
    @Json(name = "top_operators") val topOperators: List<TopCount>?,
)

@JsonClass(generateAdapter = true)
data class TopCount(
    val registration: String? = null,
    val operator: String? = null,
    val c: Int?,
)