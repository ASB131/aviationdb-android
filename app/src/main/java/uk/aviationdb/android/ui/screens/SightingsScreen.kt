package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import retrofit2.HttpException
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.api.Sighting
import uk.aviationdb.android.data.credentials.SessionStore

private const val PAGE = 50

@Composable
fun SightingsScreen(sessionStore: SessionStore) {
    var items by remember { mutableStateOf<List<Sighting>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableIntStateOf(0) }
    var end by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val signedIn = sessionStore.isSignedIn()
    val hasTracking = sessionStore.hasTracking()

    suspend fun loadMore() {
        if (loading || end || !signedIn || !hasTracking) return
        loading = true
        error = null
        try {
            val page = ApiProvider.service().sightings(limit = PAGE, offset = offset)
            val batch = page.sightings.orEmpty()
            if (batch.isEmpty()) {
                end = true
            } else {
                items = items + batch
                offset += batch.size
                if (batch.size < PAGE) end = true
            }
        } catch (e: HttpException) {
            error = when (e.code()) {
                401, 403 -> "API key invalid or subscription required."
                else -> "HTTP ${e.code()}"
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    LaunchedEffect(signedIn, hasTracking) {
        if (!signedIn) {
            error = "Please sign in again."
            return@LaunchedEffect
        }
        if (!hasTracking) {
            error = "Sightings need an active tracking subscription."
            return@LaunchedEffect
        }
        items = emptyList()
        offset = 0
        end = false
        loadMore()
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 5
        }
            .distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd) loadMore()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Your sightings", style = MaterialTheme.typography.headlineSmall)

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
        }

        if (items.isEmpty() && loading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { "${it.registration}-${it.seenAt}-${it.callsign}" }) { s ->
                    SightingCard(s)
                }
            }
        }

        if (loading && items.isNotEmpty()) {
            Text("Loading more…", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SightingCard(s: Sighting) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                listOfNotNull(s.registration, s.callsign).joinToString(" · ").ifBlank { "Sighting" },
                style = MaterialTheme.typography.titleMedium,
            )
            s.seenAt?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            val line = buildList {
                s.operator?.let { add(it) }
                s.aircraftType?.let { add(it) }
                val route = listOfNotNull(s.routeFrom, s.routeTo).joinToString(" → ")
                if (route.isNotBlank()) add(route)
                s.regionLabel?.let { add(it) }
            }.joinToString(" · ")
            if (line.isNotBlank()) {
                Text(line, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}