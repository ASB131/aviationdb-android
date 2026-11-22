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
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.components.StatTile

@Composable
fun StatsScreen(sessionStore: SessionStore) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf<StatsOverviewResponse?>(null) }

    LaunchedEffect(sessionStore.isSignedIn(), sessionStore.hasTracking()) {
        if (!sessionStore.isSignedIn() || !sessionStore.hasTracking()) {
            error = "Tracking subscription required for personal stats."
            loading = false
            return@LaunchedEffect
        }
        loading = true
        try {
            stats = ApiProvider.service().statsOverview()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = "Your stats",
            subtitle = stats?.planName ?: "Personal dashboard",
        )

        when {
            loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            stats != null -> {
                val s = stats!!
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            StatTile("Overhead", (s.activeNow ?: 0).toString(), modifier = Modifier.weight(1f))
                            StatTile("Sightings", (s.totalSightings ?: 0).toString(), modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            StatTile("Unique regs", (s.uniqueRegistrations ?: 0).toString(), modifier = Modifier.weight(1f))
                            StatTile("Callsigns", (s.uniqueCallsigns ?: 0).toString(), modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Text(
                            "${s.historyDays ?: 0} day history window",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    item {
                        AdbCard {
                            Text("Most seen registrations", style = MaterialTheme.typography.titleMedium)
                            s.topRegistrations.orEmpty().forEach {
                                Text(
                                    "${it.registration ?: "?"} · ${it.c ?: 0}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    item {
                        AdbCard {
                            Text("Top operators", style = MaterialTheme.typography.titleMedium)
                            s.topOperators.orEmpty().forEach {
                                Text(
                                    "${it.operator ?: "?"} · ${it.c ?: 0}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            "Charts, timelines, and country breakdowns: open Stats on aviationdb.uk.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}