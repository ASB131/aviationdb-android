package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import uk.aviationdb.android.ui.util.LoadState

private const val POLL_MS = 10_000L

@Composable
fun LiveScreen(sessionStore: SessionStore) {
    var state by remember { mutableStateOf<LoadState<List<ActiveFlight>>>(LoadState.Idle) }
    val signedIn = sessionStore.isSignedIn()
    val hasTracking = sessionStore.hasTracking()

    LaunchedEffect(signedIn, hasTracking) {
        if (!signedIn) {
            state = LoadState.Error("Please sign in again.")
            return@LaunchedEffect
        }
        if (!hasTracking) {
            state = LoadState.Error("Live tracking needs an active tracking subscription on your account.")
            return@LaunchedEffect
        }
        while (isActive) {
            state = LoadState.Loading
            state = try {
                val body = ApiProvider.service().activeFlights()
                LoadState.Success(body.flights)
            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    401, 403 -> "API key invalid or tracking subscription required."
                    else -> "HTTP ${e.code()}"
                }
                LoadState.Error(msg)
            } catch (e: Exception) {
                LoadState.Error(e.message ?: "Could not load live flights")
            }
            delay(POLL_MS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Live overhead", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Refreshes every ${POLL_MS / 1000}s · same feed as the site active view",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        when (val s = state) {
            LoadState.Idle, LoadState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }

            is LoadState.Error -> {
                Text(s.message, color = MaterialTheme.colorScheme.error)
            }

            is LoadState.Success -> {
                if (s.data.isEmpty()) {
                    Text("No aircraft in range right now.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(s.data, key = { it.icao24 ?: it.callsign ?: it.registration ?: it.hashCode() }) { f ->
                            FlightCard(f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightCard(f: ActiveFlight) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            val title = listOfNotNull(f.callsign, f.registration).joinToString(" · ").ifBlank { "Unknown" }
            Text(title, style = MaterialTheme.typography.titleMedium)
            val route = listOfNotNull(f.originIcao, f.destIcao).joinToString(" → ")
            if (route.isNotBlank()) {
                Text(route, style = MaterialTheme.typography.bodyMedium)
            }
            val meta = buildList {
                f.airline?.let { add(it) }
                f.aircraftType?.let { add(it) }
                if (f.altitude != null) add("${f.altitude} ft")
                if (f.speed != null) add("${f.speed} kts")
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodySmall)
            }
            if (f.lat != null && f.lon != null) {
                Text(
                    "Position ${"%.4f".format(f.lat)}, ${"%.4f".format(f.lon)}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}