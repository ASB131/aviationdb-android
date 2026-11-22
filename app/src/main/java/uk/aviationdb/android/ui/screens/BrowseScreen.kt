package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import retrofit2.HttpException
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbTopBanner

private fun formatMap(map: Map<String, Any?>): String =
    map.entries.joinToString("\n") { "${it.key}: ${it.value}" }.ifBlank { "No results." }

private enum class BrowseMode(val label: String) {
    Aircraft("Aircraft"),
    Airline("Airline"),
    Airport("Airport"),
    Callsign("Callsign"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowseScreen(sessionStore: SessionStore) {
    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(BrowseMode.Aircraft) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = "Browse",
            subtitle = "Search the AviationDB index",
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (!sessionStore.isSignedIn()) {
                Text("Sign in to search.", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrowseMode.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { mode = m },
                        label = { Text(m.label) },
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it.uppercase() },
                label = {
                    Text(
                        when (mode) {
                            BrowseMode.Aircraft -> "Registration or ICAO24"
                            BrowseMode.Airline -> "ICAO / IATA / name"
                            BrowseMode.Airport -> "ICAO or name"
                            BrowseMode.Callsign -> "Callsign"
                        },
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                singleLine = true,
            )

            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        result = null
                        try {
                            val api = ApiProvider.service()
                            result = when (mode) {
                                BrowseMode.Aircraft -> formatMap(api.aircraftShort(query.trim()))
                                BrowseMode.Airline -> formatMap(api.airlineShort(query.trim()))
                                BrowseMode.Airport -> formatMap(api.airportLookup(query.trim()))
                                BrowseMode.Callsign -> formatMap(api.callsignLookup(query.trim()))
                            }
                        } catch (e: HttpException) {
                            error = when (e.code()) {
                                404 -> "Not found."
                                else -> "HTTP ${e.code()}"
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Search failed"
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.padding(top = 12.dp),
                enabled = query.length >= 2 && !busy,
            ) { Text(if (busy) "Searching…" else "Search") }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }

            result?.let {
                AdbCard(modifier = Modifier.padding(top = 16.dp)) {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }

            AdbCard(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    "Community, FIR map, news, and full aircraft pages are on the website — this app focuses on tracking plus quick lookups.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}