package uk.aviationdb.android.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import retrofit2.HttpException
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.api.ActiveFlight
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.components.FlightRowCard
import uk.aviationdb.android.ui.live.LiveMapView
import uk.aviationdb.android.ui.live.flightKey
import uk.aviationdb.android.ui.util.LoadState

private const val POLL_MS = 10_000L

private enum class LivePane { Map, List }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(sessionStore: SessionStore) {
    var state by remember { mutableStateOf<LoadState<List<ActiveFlight>>>(LoadState.Idle) }
    var pane by remember { mutableIntStateOf(LivePane.Map.ordinal) }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    val signedIn = sessionStore.isSignedIn()
    val hasTracking = sessionStore.hasTracking()
    val homeLat = sessionStore.getTrackLat()
    val homeLon = sessionStore.getTrackLon()

    LaunchedEffect(signedIn, hasTracking) {
        if (!signedIn) {
            state = LoadState.Error("Please sign in again.")
            return@LaunchedEffect
        }
        if (!hasTracking) {
            state = LoadState.Error("Live tracking needs an active subscription. Manage plans at aviationdb.uk.")
            return@LaunchedEffect
        }
        while (isActive) {
            if (state !is LoadState.Success) state = LoadState.Loading
            state = try {
                val body = ApiProvider.service().activeFlights()
                LoadState.Success(body.flights)
            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    401, 403 -> "Session expired or tracking subscription required."
                    else -> "HTTP ${e.code()}"
                }
                LoadState.Error(msg)
            } catch (e: Exception) {
                LoadState.Error(e.message ?: "Could not load live flights")
            }
            delay(POLL_MS)
        }
    }

    val count = (state as? LoadState.Success)?.data?.size ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = "Overhead now",
            subtitle = when (val st = state) {
                is LoadState.Success -> "$count aircraft · updates every ${POLL_MS / 1000}s"
                is LoadState.Loading -> "Updating…"
                is LoadState.Error -> st.message
                else -> "Live ADS-B near your location"
            },
        )

        PrimaryTabRow(selectedTabIndex = pane) {
            Tab(
                selected = pane == LivePane.Map.ordinal,
                onClick = { pane = LivePane.Map.ordinal },
                text = { Text("Map") },
                icon = { androidx.compose.material3.Icon(Icons.Default.Map, null) },
            )
            Tab(
                selected = pane == LivePane.List.ordinal,
                onClick = { pane = LivePane.List.ordinal },
                text = { Text("List") },
                icon = { androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.List, null) },
            )
        }

        when (val s = state) {
            LoadState.Idle, LoadState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is LoadState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is LoadState.Success -> {
                if (pane == LivePane.Map.ordinal) {
                    if (s.data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Nothing overhead — map will show aircraft when they appear.")
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LiveMapView(
                                flights = s.data,
                                homeLat = homeLat,
                                homeLon = homeLon,
                                selectedKey = selectedKey,
                                onMarkerClick = { selectedKey = flightKey(it) },
                                modifier = Modifier.fillMaxSize(),
                            )
                            selectedKey?.let { key ->
                                val f = s.data.firstOrNull { flightKey(it) == key }
                                if (f != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .padding(12.dp),
                                    ) {
                                        FlightRowCard(flight = f, selected = true)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (s.data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No aircraft in range.")
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(s.data, key = { flightKey(it) }) { f ->
                                FlightRowCard(
                                    flight = f,
                                    selected = flightKey(f) == selectedKey,
                                    onClick = { selectedKey = flightKey(f) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}