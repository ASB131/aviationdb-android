package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import uk.aviationdb.android.data.api.StatsOverviewResponse
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbRankedRow
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.navigation.LocalOpenMenu
import uk.aviationdb.android.ui.components.StatTile
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StatsScreen(sessionStore: SessionStore) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf<StatsOverviewResponse?>(null) }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.UK) }

    LaunchedEffect(Unit) {
        if (!sessionStore.isSignedIn()) {
            error = "Please sign in to view stats."
            loading = false
            return@LaunchedEffect
        }
        if (!sessionStore.hasTracking()) {
            error = "An active tracking subscription is required for personal stats."
            loading = false
            return@LaunchedEffect
        }
        loading = true
        try {
            stats = ApiProvider.service().statsOverview()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Could not load stats"
            stats = null
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = "Your stats",
            onMenuClick = LocalOpenMenu.current,
            subtitle = stats?.planName?.let { "$it · ${stats?.historyDays ?: 0} day window" }
                ?: "Personal dashboard",
        )

        when {
            loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            stats != null -> {
                val s = stats!!
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            StatTile("Overhead now", numberFormat.format(s.activeNow ?: 0), modifier = Modifier.weight(1f))
                            StatTile("Sightings", numberFormat.format(s.totalSightings ?: 0), modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            StatTile(
                                "Unique regs",
                                numberFormat.format(s.uniqueRegistrations ?: 0),
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                "Callsigns",
                                numberFormat.format(s.uniqueCallsigns ?: 0),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item {
                        AdbCard {
                            Text("Most seen registrations", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            val rows = s.topRegistrations.orEmpty()
                            if (rows.isEmpty()) {
                                Text("No sightings in your history window yet.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                rows.forEachIndexed { idx, row ->
                                    AdbRankedRow(
                                        rank = idx + 1,
                                        title = row.registration ?: "Unknown",
                                        count = row.c ?: 0,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        AdbCard {
                            Text("Top operators", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            val rows = s.topOperators.orEmpty()
                            if (rows.isEmpty()) {
                                Text("No operator data in your history window yet.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                rows.forEachIndexed { idx, row ->
                                    AdbRankedRow(
                                        rank = idx + 1,
                                        title = row.operator ?: "Unknown operator",
                                        count = row.c ?: 0,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}