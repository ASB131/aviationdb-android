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
import uk.aviationdb.android.data.api.CallsignSearchResponse
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbDetailRow
import uk.aviationdb.android.ui.components.AdbTopBanner

@Composable
fun CallsignDetailScreen(callsign: String, onBack: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<CallsignSearchResponse?>(null) }

    LaunchedEffect(callsign) {
        loading = true
        try {
            data = ApiProvider.service().callsignLookup(callsign)
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    val master = data?.master

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(title = callsign.uppercase(), subtitle = "Callsign & your history", onBackClick = onBack)
        when {
            loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            else -> {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AdbCard {
                        Column {
                            val route = listOfNotNull(master?.routeFrom, master?.routeTo).joinToString(" → ")
                            if (route.isNotBlank()) Text(route, style = MaterialTheme.typography.titleMedium)
                            AdbDetailRow("Operator", master?.operator ?: "—")
                            AdbDetailRow("Sightings", (master?.callsignsTotal ?: 0).toString())
                            AdbDetailRow("Last seen", master?.lastSeenTime ?: "—")
                        }
                    }
                    Text("Recent aircraft on this callsign", style = MaterialTheme.typography.labelLarge)
                    data?.flights.orEmpty().forEach { row ->
                        AdbCard {
                            Column {
                                Text(row.registration ?: "Aircraft", style = MaterialTheme.typography.titleMedium)
                                val r = listOfNotNull(row.routeFrom, row.routeTo).joinToString(" → ")
                                if (r.isNotBlank()) Text(r, style = MaterialTheme.typography.bodyMedium)
                                row.operator?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }
    }
}