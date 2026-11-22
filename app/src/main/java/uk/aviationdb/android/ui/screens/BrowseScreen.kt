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
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import retrofit2.HttpException
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.api.AircraftBrowse
import uk.aviationdb.android.data.api.AirlineBrowse
import uk.aviationdb.android.data.api.AirportBrowse
import uk.aviationdb.android.data.api.CallsignMaster
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbDetailRow
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.navigation.LocalOpenMenu
import java.net.URLEncoder

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
    var aircraftResults by remember { mutableStateOf<List<AircraftBrowse>>(emptyList()) }
    var airline by remember { mutableStateOf<AirlineBrowse?>(null) }
    var airport by remember { mutableStateOf<AirportBrowse?>(null) }
    var callsignSummary by remember { mutableStateOf<CallsignMaster?>(null) }
    val scope = rememberCoroutineScope()

    fun clearResults() {
        aircraftResults = emptyList()
        airline = null
        airport = null
        callsignSummary = null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = "Browse",
            subtitle = "Search like the website",
            onMenuClick = LocalOpenMenu.current,
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
                            BrowseMode.Aircraft -> "Registration prefix (e.g. G-TUI)"
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
                                    aircraftResults = api.aircraftSearch(query.trim()).results.orEmpty()
                                    if (aircraftResults.isEmpty()) error = "No matching registrations."
                                }
                                BrowseMode.Airline -> {
                                    airline = api.airlineShort(query.trim()).airline
                                }
                                BrowseMode.Airport -> {
                                    airport = api.airportLookup(query.trim()).airport
                                }
                                BrowseMode.Callsign -> {
                                    callsignSummary = api.callsignLookup(query.trim()).master
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

            aircraftResults.forEach { a ->
                val reg = a.registration ?: return@forEach
                AdbCard(onClick = { navController.navigate("aircraft/$reg") }) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(reg, style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOfNotNull(a.aircraftModel, a.operator).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            airline?.let { al ->
                val code = al.icao ?: query
                AdbCard(onClick = {
                    navController.navigate("airline/${URLEncoder.encode(code, Charsets.UTF_8.name())}")
                }) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(al.name ?: code, style = MaterialTheme.typography.titleLarge)
                        Text("Tap for full airline profile", style = MaterialTheme.typography.bodySmall)
                        AdbDetailRow("ICAO / IATA", "${al.icao ?: "—"} / ${al.iata ?: "—"}")
                        AdbDetailRow("Country", al.country ?: "—")
                    }
                }
            }

            airport?.let { ap ->
                val ident = ap.ident ?: query
                AdbCard(onClick = {
                    navController.navigate("airport/${URLEncoder.encode(ident, Charsets.UTF_8.name())}")
                }) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ap.name ?: ident, style = MaterialTheme.typography.titleLarge)
                        Text("Tap for runways and full details", style = MaterialTheme.typography.bodySmall)
                        AdbDetailRow("ICAO", ident)
                        AdbDetailRow("IATA", ap.iata ?: "—")
                    }
                }
            }

            callsignSummary?.let { m ->
                val cs = m.callsign ?: query
                AdbCard(onClick = {
                    navController.navigate("callsign/${URLEncoder.encode(cs, Charsets.UTF_8.name())}")
                }) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(cs, style = MaterialTheme.typography.titleLarge)
                        Text("Tap to view your sighting history", style = MaterialTheme.typography.bodySmall)
                        val route = listOfNotNull(m.routeFrom, m.routeTo).joinToString(" → ")
                        if (route.isNotBlank()) Text(route, style = MaterialTheme.typography.bodyMedium)
                        AdbDetailRow("Operator", m.operator ?: "—")
                        AdbDetailRow("Your sightings", (m.callsignsTotal ?: 0).toString())
                    }
                }
            }
        }
    }
}