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
fun AirlineDetailScreen(icao: String, onBack: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var payload by remember { mutableStateOf<Map<String, Any?>?>(null) }

    LaunchedEffect(icao) {
        loading = true
        try {
            payload = ApiProvider.service().airlineFull(icao)
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    val airline = payload?.get("airline") as? Map<String, Any?>

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(title = icao.uppercase(), subtitle = "Airline", onBackClick = onBack)
        when {
            loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            airline != null -> {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AdbCard {
                        Column {
                            Text(airline["name"]?.toString() ?: icao, style = MaterialTheme.typography.titleLarge)
                            AdbDetailRow("ICAO", airline["icao"]?.toString() ?: "—")
                            AdbDetailRow("IATA", airline["iata"]?.toString() ?: "—")
                            AdbDetailRow("Country", airline["country"]?.toString() ?: "—")
                            AdbDetailRow("Region", airline["region"]?.toString() ?: "—")
                            AdbDetailRow("Status", airline["status"]?.toString() ?: "—")
                        }
                    }
                }
            }
        }
    }
}