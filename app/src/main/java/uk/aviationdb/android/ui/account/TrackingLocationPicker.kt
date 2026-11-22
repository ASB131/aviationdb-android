package uk.aviationdb.android.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.annotations.MarkerOptions

private const val CARTO_STYLE = "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
private const val DEFAULT_ZOOM = 10.0

@Composable
fun TrackingLocationPickerDialog(
    initialLat: Double?,
    initialLon: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    remember { MapLibre.getInstance(context) }

    val startLat = initialLat ?: 51.5074
    val startLon = initialLon ?: -0.1278
    var pickLat by remember { mutableStateOf(startLat) }
    var pickLon by remember { mutableStateOf(startLon) }
    var mapReady by remember { mutableStateOf(false) }

    val holder = remember { MapHolder() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tracking location") },
        text = {
            Column {
                Text(
                    "Tap the map to set your home area centre.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            onCreate(null)
                            getMapAsync { map ->
                                map.uiSettings.isAttributionEnabled = true
                                map.uiSettings.isLogoEnabled = false
                                map.setStyle(CARTO_STYLE) {
                                    mapReady = true
                                    holder.map = map
                                    map.animateCamera(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition.Builder()
                                                .target(LatLng(startLat, startLon))
                                                .zoom(DEFAULT_ZOOM)
                                                .build(),
                                        ),
                                    )
                                    holder.syncMarker(LatLng(pickLat, pickLon))
                                }
                                map.addOnMapClickListener { point ->
                                    pickLat = point.latitude
                                    pickLon = point.longitude
                                    holder.syncMarker(point)
                                    true
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
                )
                Text(
                    "Selected: ${"%.5f".format(pickLat)}, ${"%.5f".format(pickLon)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(pickLat, pickLon) }) { Text("Use this location") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

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
    var cleanup: (() -> Unit)? = null
    private var marker: org.maplibre.android.annotations.Marker? = null

    fun destroy() {
        mapView?.onDestroy()
        map = null
        mapView = null
    }

    fun syncMarker(at: LatLng) {
        val m = map ?: return
        marker?.let { m.removeMarker(it) }
        marker = m.addMarker(
            MarkerOptions().position(at).title("Home"),
        )
    }
}