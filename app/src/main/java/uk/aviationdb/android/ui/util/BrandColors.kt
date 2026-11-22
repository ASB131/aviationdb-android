package uk.aviationdb.android.ui.util

import androidx.compose.ui.graphics.Color
import org.json.JSONArray

fun brandAccentColor(raw: String?): Color? {
    if (raw.isNullOrBlank()) return null
  return try {
        val trimmed = raw.trim()
        val hex = when {
            trimmed.startsWith("[") -> {
                val arr = JSONArray(trimmed)
                if (arr.length() == 0) return null
                arr.optString(0).takeIf { it.isNotBlank() }
            }
            trimmed.startsWith("#") -> trimmed
            else -> null
        } ?: return null
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        null
    }
}