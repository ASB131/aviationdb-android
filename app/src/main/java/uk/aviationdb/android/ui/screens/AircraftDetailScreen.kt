package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import uk.aviationdb.android.data.api.AircraftBrowse
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbDetailRow
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.tracking.MediaUrls

@Composable
fun AircraftDetailScreen(registration: String, onBack: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var short by remember { mutableStateOf<AircraftBrowse?>(null) }
    var full by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var personal by remember { mutableStateOf<Map<String, Any?>?>(null) }

    LaunchedEffect(registration) {
        loading = true
        error = null
        try {
            short = ApiProvider.service().aircraftShort(registration).aircraft
            full = ApiProvider.service().aircraftFull(registration)
            personal = ApiProvider.service().meAircraft(registration)
        } catch (e: Exception) {
            error = e.message ?: "Could not load aircraft"
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = registration.uppercase(),
            subtitle = "Aircraft profile",
            onBackClick = onBack,
        )

        when {
            loading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            else -> Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                val imageUrl = sequenceOf(
                    full?.get("image_url") as? String,
                    full?.get("livery") as? String,
                    full?.let { (it["aircraft"] as? Map<*, *>)?.get("image_url") as? String },
                ).filterNotNull().firstOrNull()?.let { MediaUrls.absolute(it) }

                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentScale = ContentScale.Fit,
                    )
                }

                short?.let { a ->
                    AdbCard {
                        Column {
                            Text(a.registration ?: registration, style = MaterialTheme.typography.titleMedium)
                            AdbDetailRow("Model", a.aircraftModel ?: "—")
                            AdbDetailRow("Type", a.aircraftType ?: a.icaoType ?: "—")
                            AdbDetailRow("Operator", a.operator ?: "—")
                            AdbDetailRow("ICAO24", a.icao24 ?: "—")
                        }
                    }
                }

                full?.let {
                    AdbCard(modifier = Modifier.padding(top = 12.dp)) {
                        Text("Details", style = MaterialTheme.typography.titleMedium)
                        Text(formatMap(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                personal?.let {
                    AdbCard(modifier = Modifier.padding(top = 12.dp)) {
                        Text("Your sightings", style = MaterialTheme.typography.titleMedium)
                        Text(formatMap(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                Text(
                    "Full galleries and route history: aviationdb.uk",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

private fun formatMap(map: Map<String, Any?>?): String {
    if (map == null) return ""
    return map.entries.joinToString("\n") { "${it.key}: ${it.value}" }
}