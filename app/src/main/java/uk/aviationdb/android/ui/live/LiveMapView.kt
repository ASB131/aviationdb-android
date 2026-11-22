package uk.aviationdb.android.ui.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import uk.aviationdb.android.tracking.AircraftIconResolver
import uk.aviationdb.android.tracking.FlightMotionEngine
import uk.aviationdb.android.tracking.MotionPlane
import android.graphics.Color
import androidx.core.graphics.toColorInt

private const val CARTO_STYLE = "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
private const val HOME_ZOOM = 11.0
private const val FIT_PADDING = 100

@Composable
fun LiveMapView(
    motion: FlightMotionEngine,
    homeLat: Double?,
    homeLon: Double?,
    selectedKey: String?,
    refreshTick: Long,
    scope: CoroutineScope,
    onMarkerClick: (MotionPlane) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    remember { MapLibre.getInstance(context) }
    val holder = remember { MapHolder() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                onCreate(null)
                getMapAsync { map ->
                    map.uiSettings.isAttributionEnabled = true
                    map.uiSettings.isLogoEnabled = false
                    map.setStyle(CARTO_STYLE) {
                        holder.map = map
                        holder.styleReady = true
                        holder.applyHome(homeLat, homeLon)
                    }
                }
                holder.mapView = this
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> onStart()
                        Lifecycle.Event.ON_RESUME -> onResume()
                        Lifecycle.Event.ON_PAUSE -> onPause()
                        Lifecycle.Event.ON_STOP -> onStop()
                        Lifecycle.Event.ON_DESTROY -> onDestroy()
                        else -> {}
                    }
                }
                lifecycle.addObserver(observer)
                holder.cleanup = { lifecycle.removeObserver(observer) }
            }
        },
        update = {
            holder.applyHome(homeLat, homeLon)
            scope.launch {
                holder.redraw(context, motion, selectedKey, onMarkerClick, scope)
            }
        },
    )

    LaunchedEffect(homeLat, homeLon) {
        if (homeLat != null && homeLon != null) {
            holder.homeApplied = false
            holder.applyHome(homeLat, homeLon)
        }
        holder.fitUserArea(motion, homeLat, homeLon)
    }

    LaunchedEffect(refreshTick) {
        scope.launch {
            holder.redraw(context, motion, selectedKey, onMarkerClick, scope)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            holder.cleanup?.invoke()
            holder.destroy()
        }
    }
}

private class MapHolder {
    var mapView: MapView? = null
    var map: org.maplibre.android.maps.MapLibreMap? = null
    var styleReady = false
    var cleanup: (() -> Unit)? = null
    var homeApplied = false
    private val markers = mutableListOf<Marker>()
    private val polylines = mutableListOf<Polyline>()

    fun destroy() {
        mapView?.onDestroy()
        map = null
        mapView = null
    }

    fun applyHome(homeLat: Double?, homeLon: Double?) {
        val m = map ?: return
        if (!styleReady || homeLat == null || homeLon == null) return
        if (homeApplied) return
        m.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(LatLng(homeLat, homeLon)).zoom(HOME_ZOOM).build(),
            ),
        )
        homeApplied = true
    }

    fun fitUserArea(engine: FlightMotionEngine, homeLat: Double?, homeLon: Double?) {
        val m = map ?: return
        if (!styleReady) return
        val points = mutableListOf<LatLng>()
        if (homeLat != null && homeLon != null) points += LatLng(homeLat, homeLon)
        engine.all().forEach { points += LatLng(it.displayLat, it.displayLon) }
        if (points.isEmpty()) {
            if (homeLat != null && homeLon != null) {
                m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(homeLat, homeLon), HOME_ZOOM))
            }
            return
        }
        if (points.size == 1) {
            m.animateCamera(CameraUpdateFactory.newLatLngZoom(points.first(), HOME_ZOOM))
            return
        }
        val b = LatLngBounds.Builder().apply { points.forEach { include(it) } }.build()
        m.animateCamera(CameraUpdateFactory.newLatLngBounds(b, FIT_PADDING))
    }

    suspend fun redraw(
        context: android.content.Context,
        engine: FlightMotionEngine,
        selectedKey: String?,
        onClick: (MotionPlane) -> Unit,
        scope: CoroutineScope,
    ) {
        val m = map ?: return
        if (!styleReady) return

        markers.forEach { m.removeMarker(it) }
        markers.clear()
        polylines.forEach { m.removePolyline(it) }
        polylines.clear()

        engine.all().forEach { p ->
            val f = p.flight
            val variant = AircraftIconResolver.colourVariant(p.prevAlt, f.altitude)
            val iconUrl = AircraftIconResolver.radarIconUrl(f.modelIcao, f.aircraftType, variant)
            val bmp = MapIconCache.load(context, iconUrl, 72)
            val options = MarkerOptions()
                .position(LatLng(p.displayLat, p.displayLon))
                .title(f.callsign ?: f.registration ?: "Aircraft")
            if (bmp != null) {
                val iconBmp = if (p.headingDeg != 0.0) {
                    MapIconCache.rotated(bmp, p.headingDeg.toFloat())
                } else {
                    bmp
                }
                options.icon(org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(iconBmp))
            }
            val marker = m.addMarker(options)
            if (marker != null) {
                markers += marker
                if (p.key == selectedKey) m.selectMarker(marker)
            }

            if (p.trail.size >= 2) {
                val trailPts = p.trail.map { LatLng(it.first, it.second) }
                val trailLine = m.addPolyline(
                    PolylineOptions()
                        .addAll(trailPts)
                        .color("#2a6db5".toColorInt())
                        .width(2f),
                )
                if (trailLine != null) polylines += trailLine
            }

            val ahead = engine.predictAhead(p)
            if (ahead.size >= 2) {
                val predLine = m.addPolyline(
                    PolylineOptions()
                        .addAll(ahead.map { LatLng(it.first, it.second) })
                        .color(
                            when (variant) {
                                "green" -> Color.parseColor("#1b8a3e")
                                "red" -> Color.parseColor("#c81e4d")
                                else -> Color.parseColor("#b8860b")
                            },
                        )
                        .width(2f)
                        .alpha(0.75f),
                )
                if (predLine != null) polylines += predLine
            }
        }

        m.setOnMarkerClickListener { marker ->
            val hit = engine.all().firstOrNull {
                kotlin.math.abs(it.displayLat - marker.position.latitude) < 0.02 &&
                    kotlin.math.abs(it.displayLon - marker.position.longitude) < 0.02
            }
            if (hit != null) {
                onClick(hit)
                true
            } else {
                false
            }
        }
    }
}