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
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import uk.aviationdb.android.data.api.ActiveFlight

private const val CARTO_STYLE = "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"

@Composable
fun LiveMapView(
    flights: List<ActiveFlight>,
    homeLat: Double?,
    homeLon: Double?,
    selectedKey: String?,
    onMarkerClick: (ActiveFlight) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    remember { MapLibre.getInstance(context) }

    val markerState = remember { LiveMapMarkerState() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                onCreate(null)
                getMapAsync { map ->
                    map.uiSettings.isAttributionEnabled = true
                    map.uiSettings.isLogoEnabled = false
                    map.setStyle(CARTO_STYLE) {
                        markerState.map = map
                        markerState.styleLoaded = true
                        markerState.apply(flights, selectedKey, onMarkerClick)
                    }
                }
                markerState.mapView = this

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
                markerState.lifecycleCleanup = { lifecycle.removeObserver(observer) }
            }
        },
        update = {
            markerState.apply(flights, selectedKey, onMarkerClick)
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            markerState.lifecycleCleanup?.invoke()
            markerState.mapView?.onDestroy()
            markerState.clear()
        }
    }

    LaunchedEffect(homeLat, homeLon, flights.size) {
        markerState.recenterIfNeeded(homeLat, homeLon, flights)
    }
}

private class LiveMapMarkerState {
    var mapView: MapView? = null
    var map: org.maplibre.android.maps.MapLibreMap? = null
    var styleLoaded = false
    var lifecycleCleanup: (() -> Unit)? = null
    private val markers = mutableListOf<Marker>()
    private var didInitialCamera = false

    fun clear() {
        markers.clear()
        map = null
        mapView = null
    }

    fun apply(
        flights: List<ActiveFlight>,
        selectedKey: String?,
        onMarkerClick: (ActiveFlight) -> Unit,
    ) {
        val m = map ?: return
        if (!styleLoaded) return

        markers.forEach { m.removeMarker(it) }
        markers.clear()

        flights.forEach { f ->
            val lat = f.lat ?: return@forEach
            val lon = f.lon ?: return@forEach
            val key = flightKey(f)
            val title = listOfNotNull(f.callsign, f.registration).joinToString(" · ")
            val marker = m.addMarker(
                MarkerOptions()
                    .position(LatLng(lat, lon))
                    .title(title.ifBlank { "Aircraft" }),
            )
            marker?.let { mk ->
                markers.add(mk)
                if (key == selectedKey) m.selectMarker(mk)
            }
        }

        m.setOnMarkerClickListener { marker ->
            val match = flights.firstOrNull { fl ->
                fl.lat != null && fl.lon != null &&
                    kotlin.math.abs(fl.lat - marker.position.latitude) < 0.0001 &&
                    kotlin.math.abs(fl.lon - marker.position.longitude) < 0.0001
            }
            if (match != null) {
                onMarkerClick(match)
                true
            } else {
                false
            }
        }
    }

    fun recenterIfNeeded(homeLat: Double?, homeLon: Double?, flights: List<ActiveFlight>) {
        val m = map ?: return
        if (didInitialCamera) return
        if (homeLat != null && homeLon != null) {
            m.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(homeLat, homeLon)).zoom(9.0).build(),
                ),
            )
            didInitialCamera = true
            return
        }
        val points = flights.mapNotNull { f ->
            if (f.lat != null && f.lon != null) LatLng(f.lat, f.lon) else null
        }
        if (points.isEmpty()) {
            m.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(54.0, -2.5)).zoom(5.5).build(),
                ),
            )
            didInitialCamera = true
            return
        }
        if (points.size == 1) {
            m.animateCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 10.0))
        } else {
            val bounds = LatLngBounds.Builder().also { b -> points.forEach { b.include(it) } }.build()
            m.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
        }
        didInitialCamera = true
    }
}

fun flightKey(f: ActiveFlight): String =
    f.icao24 ?: f.callsign ?: f.registration ?: "${f.lat}_${f.lon}"