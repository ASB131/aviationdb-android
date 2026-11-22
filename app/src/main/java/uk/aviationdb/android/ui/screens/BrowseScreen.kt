package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import retrofit2.HttpException
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.api.AircraftBrowse
import uk.aviationdb.android.data.api.AirlineBrowse
import uk.aviationdb.android.data.api.AirportBrowse
import uk.aviationdb.android.data.api.CallsignMaster
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.tracking.MediaUrls
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbDetailRow
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.navigation.LocalOpenMenu
import uk.aviationdb.android.ui.util.brandAccentColor
import java.net.URLEncoder

private enum class BrowseMode(val label: String) {
    Aircraft("Aircraft"),
    Airline("Airline"),
    Airport("Airport"),
    Callsign("Callsign"),
}

private const val AIRCRAFT_PAGE = 25
private const val AIRLINE_PAGE = 20

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowseScreen(
    sessionStore: SessionStore,
    navController: NavHostController,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var modeName by rememberSaveable { mutableStateOf(BrowseMode.Aircraft.name) }
    val mode = remember(modeName) {
        BrowseMode.entries.firstOrNull { it.name == modeName } ?: BrowseMode.Aircraft
    }

    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }

    var aircraftResults by remember { mutableStateOf<List<AircraftBrowse>>(emptyList()) }
    var aircraftHasMore by remember { mutableStateOf(false) }
    var aircraftOffset by remember { mutableStateOf(0) }

    var airlineResults by remember { mutableStateOf<List<AirlineBrowse>>(emptyList()) }
    var airlineHasMore by remember { mutableStateOf(false) }
    var airlineOffset by remember { mutableStateOf(0) }

    var airport by remember { mutableStateOf<AirportBrowse?>(null) }
    var callsignSummary by remember { mutableStateOf<CallsignMaster?>(null) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun clearPagedResults() {
        aircraftResults = emptyList()
        aircraftHasMore = false
        aircraftOffset = 0
        airlineResults = emptyList()
        airlineHasMore = false
        airlineOffset = 0
    }

    fun clearSingles() {
        airport = null
        callsignSummary = null
    }

    suspend fun runAircraftSearch(reset: Boolean) {
        val q = query.trim()
        if (q.length < 2) return
        val offset = if (reset) 0 else aircraftOffset
        val resp = ApiProvider.service().aircraftSearch(q, limit = AIRCRAFT_PAGE, offset = offset)
        val chunk = resp.results.orEmpty()
        aircraftResults = if (reset) chunk else aircraftResults + chunk
        aircraftOffset = offset + chunk.size
        aircraftHasMore = resp.hasMore == true
        if (aircraftResults.isEmpty()) error = "No matching registrations."
    }

    suspend fun runAirlineSearch(reset: Boolean) {
        val q = query.trim()
        if (q.length < 2) return
        val offset = if (reset) 0 else airlineOffset
        val resp = ApiProvider.service().airlineSearch(q, limit = AIRLINE_PAGE, offset = offset)
        val chunk = resp.results.orEmpty()
        airlineResults = if (reset) chunk else airlineResults + chunk
        airlineOffset = offset + chunk.size
        airlineHasMore = resp.hasMore == true
        if (airlineResults.isEmpty()) error = "No matching airlines."
    }

    fun search(reset: Boolean = true) {
        scope.launch {
            if (reset) {
                busy = true
                error = null
                clearPagedResults()
                clearSingles()
            } else {
                loadingMore = true
            }
            try {
                val api = ApiProvider.service()
                when (mode) {
                    BrowseMode.Aircraft -> runAircraftSearch(reset)
                    BrowseMode.Airline -> runAirlineSearch(reset)
                    BrowseMode.Airport -> {
                        clearPagedResults()
                        airport = api.airportLookup(query.trim()).airport
                        if (airport == null) error = "Not found."
                    }
                    BrowseMode.Callsign -> {
                        clearPagedResults()
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
                loadingMore = false
            }
        }
    }

    LaunchedEffect(listState, mode, aircraftHasMore, airlineHasMore) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 4
        }.collect { nearEnd ->
            if (!nearEnd || busy || loadingMore) return@collect
            when (mode) {
                BrowseMode.Aircraft -> if (aircraftHasMore) search(reset = false)
                BrowseMode.Airline -> if (airlineHasMore) search(reset = false)
                else -> {}
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = "Browse",
            subtitle = "Search like the website",
            onMenuClick = LocalOpenMenu.current,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!sessionStore.isSignedIn()) {
                item {
                    Text("Sign in to search.", color = MaterialTheme.colorScheme.error)
                }
                return@LazyColumn
            }

            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrowseMode.entries.forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = {
                                modeName = m.name
                                error = null
                                clearPagedResults()
                                clearSingles()
                            },
                            label = { Text(m.label) },
                        )
                    }
                }
            }

            item {
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
            }

            item {
                Button(
                    onClick = { search(reset = true) },
                    enabled = query.length >= 2 && !busy,
                ) { Text(if (busy) "Searching…" else "Search") }
            }

            if (error != null) {
                item {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }

            when (mode) {
                BrowseMode.Aircraft -> {
                    items(aircraftResults, key = { it.registration ?: it.icao24 ?: it.hashCode() }) { a ->
                        val reg = a.registration ?: return@items
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
                }

                BrowseMode.Airline -> {
                    items(airlineResults, key = { it.icao ?: it.iata ?: it.name ?: it.hashCode() }) { al ->
                        val accent = brandAccentColor(al.brandColors)
                        val code = al.icao ?: al.iata ?: query
                        AdbCard(onClick = {
                            navController.navigate("airline/${URLEncoder.encode(code, Charsets.UTF_8.name())}")
                        }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (accent != null) {
                                            Modifier.background(
                                                accent.copy(alpha = 0.14f),
                                                RoundedCornerShape(8.dp),
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                al.logoUrl?.let { logo ->
                                    AsyncImage(
                                        model = MediaUrls.absolute(logo),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        al.name ?: code,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = accent ?: Color.Unspecified,
                                    )
                                    Text(
                                        "${al.icao ?: "—"} / ${al.iata ?: "—"} · ${al.country ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {}
            }

            val airportHit = airport
            if (airportHit != null) {
                item {
                    val ident = airportHit.ident ?: query
                    AdbCard(onClick = {
                        navController.navigate("airport/${URLEncoder.encode(ident, Charsets.UTF_8.name())}")
                    }) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(airportHit.name ?: ident, style = MaterialTheme.typography.titleLarge)
                            Text("Tap for runways and full details", style = MaterialTheme.typography.bodySmall)
                            AdbDetailRow("ICAO", ident)
                            AdbDetailRow("IATA", airportHit.iata ?: "—")
                        }
                    }
                }
            }

            val callsignHit = callsignSummary
            if (callsignHit != null) {
                item {
                    val cs = callsignHit.callsign ?: query
                    AdbCard(onClick = {
                        navController.navigate("callsign/${URLEncoder.encode(cs, Charsets.UTF_8.name())}")
                    }) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(cs, style = MaterialTheme.typography.titleLarge)
                            Text("Tap to view your sighting history", style = MaterialTheme.typography.bodySmall)
                            val route = listOfNotNull(callsignHit.routeFrom, callsignHit.routeTo).joinToString(" → ")
                            if (route.isNotBlank()) Text(route, style = MaterialTheme.typography.bodyMedium)
                            AdbDetailRow("Operator", callsignHit.operator ?: "—")
                            AdbDetailRow("Your sightings", (callsignHit.callsignsTotal ?: 0).toString())
                        }
                    }
                }
            }

            if (loadingMore) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}
