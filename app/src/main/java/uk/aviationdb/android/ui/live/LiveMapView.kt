package uk.aviationdb.android.ui.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import uk.aviationdb.android.tracking.AircraftIconResolver
import uk.aviationdb.android.tracking.FlightMotionEngine
import uk.aviationdb.android.tracking.MotionPlane
import kotlin.math.abs

private const val CARTO_STYLE = "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
private const val HOME_ZOOM = 11.0
private const val FIT_PADDING = 100
private const val ICON_PX = 92

@Composable
fun LiveMapView(
    motion: FlightMotionEngine,
    homeLat: Double?,
    homeLon: Double?,
    selectedKey: String?,
    syncTick: Long,
    positionTick: Long,
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
        },
    )

    LaunchedEffect(homeLat, homeLon) {
        if (homeLat != null && homeLon != null) {
            holder.homeApplied = false
            holder.applyHome(homeLat, homeLon)
        }
        holder.fitUserArea(motion, homeLat, homeLon)
    }

    LaunchedEffect(syncTick, selectedKey) {
        scope.launch {
            holder.syncMarkers(context, motion, selectedKey, onMarkerClick)
        }
    }

    LaunchedEffect(positionTick) {
        holder.updatePositions(motion, selectedKey)
    }

    DisposableEffect(Unit) {
        onDispose {
            holder.cleanup?.invoke()
            holder.destroy()
        }
    }
}

private data class MarkerVisual(
    val variant: String,
    val iconUrl: String,
    val heading: Int,
)

private class MapHolder {
    var mapView: MapView? = null
    var map: org.maplibre.android.maps.MapLibreMap? = null
    var styleReady = false
    var cleanup: (() -> Unit)? = null
    var homeApplied = false
    private val markerByKey = linkedMapOf<String, Marker>()
    private val visualByKey = mutableMapOf<String, MarkerVisual>()
    private var clickHandler: ((MotionPlane) -> Unit)? = null

    fun destroy() {
        mapView?.onDestroy()
        map = null
        mapView = null
        markerByKey.clear()
        visualByKey.clear()
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

    fun updatePositions(engine: FlightMotionEngine, selectedKey: String?) {
        val m = map ?: return
        if (!styleReady) return
        engine.all().forEach { p ->
            val marker = markerByKey[p.key] ?: return@forEach
            marker.position = LatLng(p.displayLat, p.displayLon)
            if (p.key == selectedKey) m.selectMarker(marker)
        }
    }

    suspend fun syncMarkers(
        context: android.content.Context,
        engine: FlightMotionEngine,
        selectedKey: String?,
        onClick: (MotionPlane) -> Unit,
    ) {
        val m = map ?: return
        if (!styleReady) return
        clickHandler = onClick

        val liveKeys = engine.all().associateBy { it.key }
        val remove = markerByKey.keys.filter { it !in liveKeys }
        remove.forEach { key ->
            markerByKey.remove(key)?.let { m.removeMarker(it) }
            visualByKey.remove(key)
        }

        engine.all().forEach { p ->
            val f = p.flight
            val variant = AircraftIconResolver.colourVariant(p.prevAlt, f.altitude)
            val iconUrl = AircraftIconResolver.radarIconUrl(f.modelIcao, f.aircraftType, variant)
            val heading = p.headingDeg.toInt()
            val needIcon = visualByKey[p.key]?.let {
                it.variant != variant || it.iconUrl != iconUrl || abs(it.heading - heading) >= 4
            } ?: true

            val existing = markerByKey[p.key]
            if (existing == null) {
                val options = MarkerOptions()
                    .position(LatLng(p.displayLat, p.displayLon))
                    .title(f.callsign ?: f.registration ?: "Aircraft")
                awaitIcon(context, options, iconUrl, heading)
                val marker = m.addMarker(options)
                if (marker != null) {
                    markerByKey[p.key] = marker
                    visualByKey[p.key] = MarkerVisual(variant, iconUrl, heading)
                    if (p.key == selectedKey) m.selectMarker(marker)
                }
            } else {
                existing.position = LatLng(p.displayLat, p.displayLon)
                existing.title = f.callsign ?: f.registration ?: "Aircraft"
                if (needIcon) {
                    val options = MarkerOptions().position(existing.position)
                    awaitIcon(context, options, iconUrl, heading)
                    options.icon?.let { existing.icon = it }
                    visualByKey[p.key] = MarkerVisual(variant, iconUrl, heading)
                }
                if (p.key == selectedKey) m.selectMarker(existing)
            }
        }

        m.setOnMarkerClickListener { marker ->
            val hit = engine.all().firstOrNull {
                markerByKey[it.key] === marker
            }
            if (hit != null) {
                clickHandler?.invoke(hit)
                true
            } else {
                false
            }
        }
    }

    private suspend fun awaitIcon(
        context: android.content.Context,
        options: MarkerOptions,
        iconUrl: String,
        heading: Int,
    ) {
        val bmp = MapIconCache.load(context, iconUrl, ICON_PX) ?: return
        var iconBmp = MapIconCache.withDropShadow(bmp)
        if (heading != 0) {
            iconBmp = MapIconCache.rotated(iconBmp, heading.toFloat())
        }
        options.icon(org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(iconBmp))
    }
}