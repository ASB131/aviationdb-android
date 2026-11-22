package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import retrofit2.HttpException
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.api.SummaryResponse
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.components.StatTile

@Composable
fun StatsScreen(sessionStore: SessionStore) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var summary by remember { mutableStateOf<SummaryResponse?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        error = null
        try {
            summary = ApiProvider.service().summary()
        } catch (e: HttpException) {
            error = when (e.code()) {
                403 -> "Stats need an active tracking plan."
                else -> "HTTP ${e.code()}"
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    LaunchedEffect(sessionStore.isSignedIn(), sessionStore.hasTracking()) {
        if (!sessionStore.isSignedIn()) {
            error = "Sign in required."
            loading = false
            return@LaunchedEffect
        }
        if (!sessionStore.hasTracking()) {
            error = "Subscribe to tracking on aviationdb.uk for personal stats."
            loading = false
            return@LaunchedEffect
        }
        load()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = "Your stats",
            subtitle = "Personal tracking overview",
        )

        when {
            loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = { scope.launch { load() } },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text("Retry") }
                }
            }

            summary != null -> {
                val s = summary!!
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { StatTile("Overhead now", (s.activeNow ?: 0).toString()) }
                    item { StatTile("Sightings", (s.totalSightings ?: 0).toString()) }
                    item { StatTile("Unique regs", (s.uniqueRegistrations ?: 0).toString()) }
                    item { StatTile("History days", (s.plan?.historyDays ?: 0).toString()) }
                }
                AdbCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        "Full charts and exports live on aviationdb.uk — use Stats in the browser for the complete dashboard.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}