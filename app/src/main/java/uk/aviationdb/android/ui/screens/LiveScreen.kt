package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import retrofit2.HttpException
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.tracking.FlightMotionEngine
import uk.aviationdb.android.tracking.MediaUrls
import uk.aviationdb.android.tracking.MotionPlane
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.components.FlightRowCard
import uk.aviationdb.android.ui.live.LiveMapView
import uk.aviationdb.android.ui.util.LoadState

private const val POLL_MS = 10_000L

private enum class LivePane { Map, List }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(
    sessionStore: SessionStore,
    navController: NavHostController,
) {
    var state by remember { mutableStateOf<LoadState<List<MotionPlane>>>(LoadState.Idle) }
    var pane by remember { mutableIntStateOf(LivePane.Map.ordinal) }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    var homeLat by remember { mutableStateOf(sessionStore.getTrackLat()) }
    var homeLon by remember { mutableStateOf(sessionStore.getTrackLon()) }
    var mapTick by remember { mutableLongStateOf(0L) }
    val motion = remember { FlightMotionEngine() }
    val scope = rememberCoroutineScope()
    val signedIn = sessionStore.isSignedIn()
    val hasTracking = sessionStore.hasTracking()

    LaunchedEffect(Unit) {
        while (isActive) {
            motion.step()
            mapTick = System.currentTimeMillis()
            delay(50L)
        }
    }

    LaunchedEffect(signedIn, hasTracking) {
        if (!signedIn) {
            state = LoadState.Error("Please sign in again.")
            return@LaunchedEffect
        }
        if (!hasTracking) {
            state = LoadState.Error("Live tracking needs an active subscription.")
            return@LaunchedEffect
        }
        while (isActive) {
            if (state !is LoadState.Success) state = LoadState.Loading
            state = try {
                val body = ApiProvider.service().activeFlights()
                body.trackLat?.let {
                    homeLat = it
                    homeLon = body.trackLon
                    sessionStore.updateProfile(
                        email = sessionStore.getUserEmail().orEmpty(),
                        displayName = sessionStore.getDisplayName(),
                        hasTracking = sessionStore.hasTracking(),
                        trackLat = it,
                        trackLon = body.trackLon,
                    )
                }
                motion.sync(body.flights)
                LoadState.Success(motion.all().toList())
            } catch (e: HttpException) {
                LoadState.Error(
                    when (e.code()) {
                        401, 403 -> "Session expired or tracking subscription required."
                        else -> "HTTP ${e.code()}"
                    },
                )
            } catch (e: Exception) {
                LoadState.Error(e.message ?: "Could not load live flights")
            }
            delay(POLL_MS)
        }
    }

    val count = (state as? LoadState.Success)?.data?.size ?: 0
    val listPlanes = (state as? LoadState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = "Overhead now",
            subtitle = when (val st = state) {
                is LoadState.Success -> "$count aircraft · updates every ${POLL_MS / 1000}s"
                is LoadState.Loading -> "Updating…"
                is LoadState.Error -> st.message
                else -> "Map centred on your tracking location"
            },
        )

        PrimaryTabRow(selectedTabIndex = pane, modifier = Modifier.fillMaxWidth()) {
            Tab(
                selected = pane == LivePane.Map.ordinal,
                onClick = { pane = LivePane.Map.ordinal },
                text = { Text("Map") },
                icon = { Icon(Icons.Default.Map, contentDescription = null) },
            )
            Tab(
                selected = pane == LivePane.List.ordinal,
                onClick = { pane = LivePane.List.ordinal },
                text = { Text("List") },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (pane == LivePane.List.ordinal) 0.48f else 1f),
        ) {
            if (state is LoadState.Loading && listPlanes.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LiveMapView(
                    motion = motion,
                    homeLat = homeLat,
                    homeLon = homeLon,
                    selectedKey = selectedKey,
                    refreshTick = mapTick,
                    scope = scope,
                    onMarkerClick = { p ->
                        selectedKey = p.key
                        val reg = p.flight.registration
                        if (!reg.isNullOrBlank()) {
                            navController.navigate("aircraft/${reg.trim()}")
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (state is LoadState.Error && listPlanes.isEmpty()) {
                Text(
                    (state as LoadState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                )
            }
        }

        when (pane) {
            LivePane.Map.ordinal -> {
                selectedKey?.let { key ->
                    listPlanes.firstOrNull { it.key == key }?.let { mp ->
                        FlightRowCard(
                            flight = mp.flight,
                            imageUrl = MediaUrls.listArtwork(mp.flight.media),
                            selected = true,
                            onClick = {
                                mp.flight.registration?.let { navController.navigate("aircraft/$it") }
                            },
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            LivePane.List.ordinal -> {
                if (listPlanes.isEmpty()) {
                    Text(
                        "No aircraft overhead — map stays on your tracking area.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.52f),
                    ) {
                        items(listPlanes, key = { it.key }) { mp ->
                            FlightRowCard(
                                flight = mp.flight,
                                imageUrl = MediaUrls.listArtwork(mp.flight.media),
                                selected = mp.key == selectedKey,
                                onClick = {
                                    selectedKey = mp.key
                                    mp.flight.registration?.let { navController.navigate("aircraft/$it") }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
