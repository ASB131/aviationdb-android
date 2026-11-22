package uk.aviationdb.android.tracking

import uk.aviationdb.android.data.api.ActiveFlight
import kotlin.math.cos
import kotlin.math.sin

data class MotionPlane(
    val key: String,
    var flight: ActiveFlight,
    var displayLat: Double,
    var displayLon: Double,
    var headingDeg: Double,
    var speedKts: Double,
    var fixAtMs: Long,
    var anchorLat: Double,
    var anchorLon: Double,
    var prevAlt: Int?,
    val trail: ArrayDeque<Pair<Double, Double>> = ArrayDeque(),
)

/**
 * Lightweight dead-reckoning between 10s API polls (inspired by tracking-shared.js).
 */
class FlightMotionEngine(
    private val trackerSec: Int = 20,
    private val trailMax: Int = 150,
) {
    private val planes = linkedMapOf<String, MotionPlane>()

    fun sync(flights: List<ActiveFlight>, nowMs: Long = System.currentTimeMillis()) {
        val seen = HashSet<String>()
        flights.forEach { f ->
            val key = f.icao24 ?: f.registration ?: f.callsign ?: return@forEach
            val lat = f.lat ?: return@forEach
            val lon = f.lon ?: return@forEach
            seen += key
            val existing = planes[key]
            if (existing == null) {
                planes[key] = MotionPlane(
                    key = key,
                    flight = f,
                    displayLat = lat,
                    displayLon = lon,
                    headingDeg = (f.heading ?: 0).toDouble(),
                    speedKts = (f.speed ?: 0).toDouble(),
                    fixAtMs = nowMs,
                    anchorLat = lat,
                    anchorLon = lon,
                    prevAlt = f.altitude,
                ).also { it.trail.addLast(lat to lon) }
            } else {
                val jump = haversineM(existing.anchorLat, existing.anchorLon, lat, lon)
                if (jump > 800) {
                    existing.trail.clear()
                }
                existing.prevAlt = existing.flight.altitude
                existing.flight = f
                existing.anchorLat = lat
                existing.anchorLon = lon
                existing.fixAtMs = nowMs
                existing.speedKts = (f.speed ?: existing.speedKts).toDouble()
                existing.headingDeg = (f.heading ?: existing.headingDeg).toDouble()
                existing.trail.addLast(lat to lon)
                while (existing.trail.size > trailMax) existing.trail.removeFirst()
            }
        }
        planes.keys.retainAll(seen)
    }

    fun step(nowMs: Long = System.currentTimeMillis()) {
        val extrapCapMs = (trackerSec + 12).coerceAtMost(90) * 1000L
        planes.values.forEach { p ->
            val elapsed = (nowMs - p.fixAtMs).coerceAtMost(extrapCapMs)
            if (elapsed <= 0) return@forEach
            val dt = elapsed / 1000.0
            val speedMs = p.speedKts * 0.514444 * 0.88
            val (nLat, nLon) = deadReckon(p.anchorLat, p.anchorLon, p.headingDeg, speedMs * dt)
            p.displayLat = nLat
            p.displayLon = nLon
            val last = p.trail.lastOrNull()
            if (last == null) {
                p.trail.addLast(nLat to nLon)
            } else {
                val moved = haversineM(last.first, last.second, nLat, nLon)
                if (moved >= 12) {
                    p.trail.addLast(nLat to nLon)
                    while (p.trail.size > trailMax) p.trail.removeFirst()
                }
            }
        }
    }

    /** Trail vertices for map draw: anchored history ending at interpolated display position. */
    fun trailForDraw(p: MotionPlane): List<Pair<Double, Double>> {
        if (p.trail.isEmpty()) return emptyList()
        val pts = p.trail.toMutableList()
        pts[pts.lastIndex] = p.displayLat to p.displayLon
        if (pts.size == 1) {
            val stub = deadReckon(p.displayLat, p.displayLon, p.headingDeg + 180.0, 4.0)
            return listOf(stub, p.displayLat to p.displayLon)
        }
        return pts
    }

    fun all(): Collection<MotionPlane> = planes.values

    fun get(key: String): MotionPlane? = planes[key]

    private fun deadReckon(lat: Double, lon: Double, headingDeg: Double, distM: Double): Pair<Double, Double> {
        val rad = Math.toRadians(headingDeg)
        val dLat = distM * cos(rad) / EARTH_RADIUS_M * (180.0 / Math.PI)
        val dLon = distM * sin(rad) / (EARTH_RADIUS_M * cos(Math.toRadians(lat))) * (180.0 / Math.PI)
        return lat + dLat to lon + dLon
    }

    private fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = EARTH_RADIUS_M
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }

    companion object {
        private const val EARTH_RADIUS_M = 6378137.0
    }
}