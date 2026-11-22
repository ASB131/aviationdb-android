package uk.aviationdb.android.tracking

import uk.aviationdb.android.BuildConfig

object AircraftIconResolver {
    fun slug(modelIcao: String?, aircraftType: String?): String {
        val code = (modelIcao ?: aircraftType)?.uppercase()?.trim().orEmpty()
        if (code.isBlank() || code in listOf("N/A", "UNK", "UNKNOWN")) return "b737"
        val compact = code.replace(Regex("[^A-Z0-9]"), "")
        when {
            compact.startsWith("A388") || compact.contains("A380") -> return "a380"
            compact.startsWith("B74") || compact == "B748" -> return "b747"
            compact.startsWith("B77") -> return "b777"
            compact.startsWith("B78") -> return "b787"
            compact.startsWith("B76") -> return "b767"
            compact.startsWith("B73") || compact.contains("7M") -> return "b737"
            compact.startsWith("A32") || compact.startsWith("A21") || compact.startsWith("A20") -> return "a320"
            compact.startsWith("A33") -> return "a330"
            compact.startsWith("A34") -> return "a340"
            compact.startsWith("E19") || compact.startsWith("E75") -> return "e195"
            compact.startsWith("CRJ") -> return "crjx"
            compact.startsWith("DH8") -> return "dh8a"
            compact.contains("C172") || compact.contains("C182") -> return "cessna"
        }
        return "b737"
    }

    fun colourVariant(previousAlt: Int?, altitude: Int?): String = when {
        previousAlt == null || altitude == null -> "yellow"
        altitude - previousAlt > 120 -> "green"
        altitude - previousAlt < -120 -> "red"
        else -> "yellow"
    }

    fun radarIconUrl(modelIcao: String?, aircraftType: String?, variant: String): String {
        val slug = slug(modelIcao, aircraftType)
        val base = BuildConfig.SITE_BASE_URL.trimEnd('/')
        return "$base/assets/aircraft-icons/adsb-radar/${slug}_${variant}.svg?v=11"
    }
}