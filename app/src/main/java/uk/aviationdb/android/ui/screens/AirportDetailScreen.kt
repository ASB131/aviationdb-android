package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbDetailRow
import uk.aviationdb.android.ui.components.AdbTopBanner

@Composable
fun AirportDetailScreen(ident: String, onBack: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var payload by remember { mutableStateOf<Map<String, Any?>?>(null) }

    LaunchedEffect(ident) {
        loading = true
        try {
            payload = ApiProvider.service().airportFull(ident)
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    val airport = payload?.get("airport") as? Map<String, Any?>
    @Suppress("UNCHECKED_CAST")
    val runways = payload?.get("runways") as? List<Map<String, Any?>>

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(title = ident.uppercase(), subtitle = "Airport", onBackClick = onBack)
        when {
            loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            airport != null -> {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AdbCard {
                        Column {
                            Text(airport["name"]?.toString() ?: ident, style = MaterialTheme.typography.titleLarge)
                            AdbDetailRow("ICAO", airport["ident"]?.toString() ?: ident)
                            AdbDetailRow("IATA", airport["iata"]?.toString() ?: "—")
                            AdbDetailRow("Country", airport["iso_country"]?.toString() ?: "—")
                            AdbDetailRow("Municipality", airport["municipality"]?.toString() ?: "—")
                            AdbDetailRow("Elevation", airport["elevation_ft"]?.toString()?.let { "$it ft" } ?: "—")
                        }
                    }
                    AdbCard {
                        Text("Runways (${runways?.size ?: 0})", style = MaterialTheme.typography.titleMedium)
                        runways.orEmpty().take(12).forEach { rw ->
                            val line = listOfNotNull(
                                rw["ident"]?.toString(),
                                rw["length_ft"]?.toString()?.let { "${it} ft" },
                            ).joinToString(" · ")
                            Text(line, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
}