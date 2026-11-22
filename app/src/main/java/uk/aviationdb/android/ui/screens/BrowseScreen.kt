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
import androidx.compose.material3.OutlinedButton
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
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import retrofit2.HttpException
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.api.AircraftBrowse
import uk.aviationdb.android.data.api.AirlineBrowse
import uk.aviationdb.android.data.api.AirportBrowse
import uk.aviationdb.android.data.api.CallsignSearchResponse
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbDetailRow
import uk.aviationdb.android.ui.components.AdbTopBanner

private enum class BrowseMode(val label: String) {
    Aircraft("Aircraft"),
    Airline("Airline"),
    Airport("Airport"),
    Callsign("Callsign"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowseScreen(
    sessionStore: SessionStore,
    navController: NavHostController,
) {
    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(BrowseMode.Aircraft) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var aircraft by remember { mutableStateOf<AircraftBrowse?>(null) }
    var airline by remember { mutableStateOf<AirlineBrowse?>(null) }
    var airport by remember { mutableStateOf<AirportBrowse?>(null) }
    var callsign by remember { mutableStateOf<CallsignSearchResponse?>(null) }
    val scope = rememberCoroutineScope()

    fun clearResults() {
        aircraft = null
        airline = null
        airport = null
        callsign = null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = "Browse",
            subtitle = "Aircraft, airlines, airports, and callsigns",
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!sessionStore.isSignedIn()) {
                Text("Sign in to search.", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrowseMode.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = {
                            mode = m
                            clearResults()
                            error = null
                        },
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        clearResults()
                        try {
                            val api = ApiProvider.service()
                            when (mode) {
                                BrowseMode.Aircraft -> {
                                    aircraft = api.aircraftShort(query.trim()).aircraft
                                }
                                BrowseMode.Airline -> {
                                    airline = api.airlineShort(query.trim()).airline
                                }
                                BrowseMode.Airport -> {
                                    airport = api.airportLookup(query.trim()).airport
                                }
                                BrowseMode.Callsign -> {
                                    callsign = api.callsignLookup(query.trim())
                                }
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
                enabled = query.length >= 2 && !busy,
            ) { Text(if (busy) "Searching…" else "Search") }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            aircraft?.let { a ->
                AdbCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(a.registration ?: "Aircraft", style = MaterialTheme.typography.titleLarge)
                        listOfNotNull(a.aircraftModel, a.operator).joinToString(" · ").takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        AdbDetailRow("Type", a.aircraftType ?: a.icaoType ?: "—")
                        AdbDetailRow("ICAO24", a.icao24 ?: "—")
                        AdbDetailRow("Operator", a.operator ?: "—")
                        AdbDetailRow("Airline code", a.airlineCode ?: "—")
                    }
                }
                a.registration?.let { reg ->
                    OutlinedButton(
                        onClick = { navController.navigate("aircraft/$reg") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Open aircraft page") }
                }
            }

            airline?.let { al ->
                AdbCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(al.name ?: al.icao ?: "Airline", style = MaterialTheme.typography.titleLarge)
                        AdbDetailRow("ICAO", al.icao ?: "—")
                        AdbDetailRow("IATA", al.iata ?: "—")
                        AdbDetailRow("Country", al.country ?: "—")
                        AdbDetailRow("Region", al.region ?: "—")
                        AdbDetailRow("Status", al.status ?: "—")
                    }
                }
            }

            airport?.let { ap ->
                AdbCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ap.name ?: ap.ident ?: "Airport", style = MaterialTheme.typography.titleLarge)
                        AdbDetailRow("ICAO", ap.ident ?: "—")
                        AdbDetailRow("IATA", ap.iata ?: "—")
                        AdbDetailRow("Country", ap.isoCountry ?: "—")
                        AdbDetailRow("Municipality", ap.municipality ?: "—")
                        AdbDetailRow("FIR", ap.FIR ?: "—")
                        AdbDetailRow("Elevation", ap.elevationFt?.let { "$it ft" } ?: "—")
                        AdbDetailRow("Coordinates", ap.coordinates ?: "—")
                    }
                }
            }

            callsign?.let { cs ->
                val m = cs.master
                AdbCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(m?.callsign ?: query, style = MaterialTheme.typography.titleLarge)
                        val route = listOfNotNull(m?.routeFrom, m?.routeTo).joinToString(" → ")
                        if (route.isNotBlank()) {
                            Text(route, style = MaterialTheme.typography.bodyLarge)
                        }
                        AdbDetailRow("Operator", m?.operator ?: "—")
                        AdbDetailRow("Your sightings", (m?.callsignsTotal ?: 0).toString())
                        AdbDetailRow("Last seen", m?.lastSeenTime ?: "—")
                    }
                }
                cs.flights.orEmpty().take(6).forEach { row ->
                    AdbCard {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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