package uk.aviationdb.android.data.credentials

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getAccessToken(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun getUserEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun getDisplayName(): String? = prefs.getString(KEY_NAME, null)

    fun hasTracking(): Boolean = prefs.getBoolean(KEY_TRACKING, false)

    fun getTrackLat(): Double? =
        prefs.getString(KEY_LAT, null)?.toDoubleOrNull()

    fun getTrackLon(): Double? =
        prefs.getString(KEY_LON, null)?.toDoubleOrNull()

    fun saveSession(
        token: String,
        email: String,
        displayName: String?,
        hasTracking: Boolean,
        trackLat: Double?,
        trackLon: Double?,
    ) {
        prefs.edit()
            .putString(KEY_TOKEN, token.trim())
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, displayName.orEmpty())
            .putBoolean(KEY_TRACKING, hasTracking)
            .apply {
                if (trackLat != null) putString(KEY_LAT, trackLat.toString()) else remove(KEY_LAT)
                if (trackLon != null) putString(KEY_LON, trackLon.toString()) else remove(KEY_LON)
            }
            .apply()
    }

    fun updateProfile(
        email: String,
        displayName: String?,
        hasTracking: Boolean,
        trackLat: Double? = getTrackLat(),
        trackLon: Double? = getTrackLon(),
    ) {
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, displayName.orEmpty())
            .putBoolean(KEY_TRACKING, hasTracking)
            .apply {
                if (trackLat != null) putString(KEY_LAT, trackLat.toString()) else remove(KEY_LAT)
                if (trackLon != null) putString(KEY_LON, trackLon.toString()) else remove(KEY_LON)
            }
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isSignedIn(): Boolean = getAccessToken() != null

    companion object {
        private const val PREFS = "aviationdb_session"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "display_name"
        private const val KEY_TRACKING = "has_tracking"
        private const val KEY_LAT = "track_lat"
        private const val KEY_LON = "track_lon"
    }
}