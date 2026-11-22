package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.PlanespottersPhotos
import uk.aviationdb.android.data.api.MeAircraftResponse
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbDetailRow
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.navigation.LocalOpenMenu
import uk.aviationdb.android.tracking.MediaUrls

@Composable
fun AircraftDetailScreen(registration: String, onBack: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<MeAircraftResponse?>(null) }
    var spotterPhotos by remember(registration) { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(registration) {
        spotterPhotos = PlanespottersPhotos.photoUrls(registration, max = 12)
    }

    LaunchedEffect(registration) {
        loading = true
        error = null
        try {
            data = ApiProvider.service().meAircraft(registration, limit = 25)
        } catch (e: Exception) {
            error = e.message ?: "Could not load aircraft"
        } finally {
            loading = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun fullMap(): Map<String, Any?>? = data?.aircraftFull
    val media = fullMap()?.get("media") as? Map<String, Any?>
    val liveryUrl = (media?.get("livery_path") as? String)?.let { MediaUrls.absolute(it) }

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = registration.uppercase(),
            subtitle = data?.aircraft?.aircraftModel ?: "Aircraft profile",
            onBackClick = onBack,
            onMenuClick = LocalOpenMenu.current,
        )

        when {
            loading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            data != null -> {
                val d = data!!
                val a = d.aircraft
                val full = fullMap()
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val gallery = buildList {
                        liveryUrl?.let { add(it) }
                        spotterPhotos.forEach { url -> if (url !in this) add(url) }
                    }
                    if (gallery.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            gallery.forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(width = 280.dp, height = 158.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                        Text(
                            "Swipe for livery and spotting photos",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    AdbCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(a?.registration ?: registration, style = MaterialTheme.typography.titleLarge)
                            AdbDetailRow("Model", a?.aircraftModel ?: full?.get("manufacturer_and_model")?.toString() ?: "—")
                            AdbDetailRow("Type", a?.aircraftType ?: full?.get("model_icao")?.toString() ?: "—")
                            AdbDetailRow("Operator", a?.operator ?: full?.get("operator")?.toString() ?: "—")
                            AdbDetailRow("ICAO24", a?.icao24 ?: full?.get("icao24")?.toString() ?: "—")
                            AdbDetailRow("Serial", full?.get("serial_number")?.toString() ?: "—")
                            AdbDetailRow("Year built", full?.get("year_built")?.toString() ?: "—")
                        }
                    }

                    d.yourStats?.let { st ->
                        AdbCard {
                            Text("Your logbook (${d.historyDays ?: 0} days)", style = MaterialTheme.typography.titleMedium)
                            AdbDetailRow("Sightings", (st.totalSightings ?: 0).toString())
                            AdbDetailRow("Callsigns", (st.uniqueCallsigns ?: 0).toString())
                            AdbDetailRow("First seen", st.firstSeen ?: "—")
                            AdbDetailRow("Last seen", st.lastSeen ?: "—")
                        }
                    }

                    val routes = d.routes.orEmpty()
                    if (routes.isNotEmpty()) {
                        AdbCard {
                            Text("Routes you've logged", style = MaterialTheme.typography.titleMedium)
                            routes.forEach { r ->
                                val line = listOfNotNull(r.routeFrom, r.routeTo).joinToString(" → ")
                                Text(
                                    "$line · ${r.timesSeen ?: 0}× · last ${r.lastSeen ?: "—"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }

                    val sightings = d.sightings.orEmpty()
                    if (sightings.isNotEmpty()) {
                        Text("Recent sightings", style = MaterialTheme.typography.titleMedium)
                        sightings.take(15).forEach { s ->
                            AdbCard {
                                Column {
                                    Text(
                                        listOfNotNull(s.callsign, s.seenAt).joinToString(" · "),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    val route = listOfNotNull(s.routeFrom, s.routeTo).joinToString(" → ")
                                    if (route.isNotBlank()) Text(route, style = MaterialTheme.typography.bodySmall)
                                    s.operator?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}