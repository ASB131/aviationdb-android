package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import uk.aviationdb.android.data.api.StatsChartRow
import uk.aviationdb.android.data.api.StatsSummaryResponse
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbDetailRow
import uk.aviationdb.android.ui.components.AdbRankedRow
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.components.StatTile
import uk.aviationdb.android.ui.navigation.LocalOpenMenu
import java.text.NumberFormat
import java.util.Locale

private data class RangeOption(val key: String, val label: String)

private val defaultRanges = listOf(
    RangeOption("today", "Today"),
    RangeOption("7d", "7 days"),
    RangeOption("30d", "30 days"),
    RangeOption("90d", "90 days"),
    RangeOption("all", "All history"),
)

private fun pctSuffix(v: Double?): String {
    if (v == null) return ""
    return if (v >= 0) " (+${"%.0f".format(v)}%)" else " (${"%.0f".format(v)}%)"
}

private fun chartLabel(row: StatsChartRow): String =
    row.label ?: row.operator ?: "?"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(sessionStore: SessionStore) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var range by remember { mutableStateOf("today") }
    var summary by remember { mutableStateOf<StatsSummaryResponse?>(null) }
    var operators by remember { mutableStateOf<List<StatsChartRow>>(emptyList()) }
    var types by remember { mutableStateOf<List<StatsChartRow>>(emptyList()) }
    val nf = remember { NumberFormat.getIntegerInstance(Locale.UK) }

    LaunchedEffect(sessionStore.isSignedIn(), sessionStore.hasTracking(), range) {
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
            val api = ApiProvider.service()
            summary = api.statsSummary(range)
            operators = api.statsCharts("operators", range).rows.orEmpty()
            types = api.statsCharts("types", range).rows.orEmpty()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Could not load stats"
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = "Your stats",
            onMenuClick = LocalOpenMenu.current,
            subtitle = summary?.let { "${it.historyDays ?: 0} day plan · range ${it.range ?: range}" }
                ?: "Personal dashboard",
        )

        when {
            loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            summary != null -> {
                val s = summary!!
                val cur = s.current
                val ch = s.change
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            defaultRanges.forEach { opt ->
                                FilterChip(
                                    selected = range == opt.key,
                                    onClick = { range = opt.key },
                                    label = { Text(opt.label) },
                                )
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            StatTile(
                                "Sightings",
                                nf.format(cur?.totalFlights ?: 0) + pctSuffix(ch?.totalFlights),
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                "Aircraft",
                                nf.format(cur?.totalAircraft ?: 0) + pctSuffix(ch?.totalAircraft),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            StatTile(
                                "Callsigns",
                                nf.format(cur?.totalCallsigns ?: 0) + pctSuffix(ch?.totalCallsigns),
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                "New to you",
                                nf.format(cur?.newAircraft ?: 0),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item {
                        AdbCard {
                            Text("Highlights", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            AdbDetailRow(
                                "Busiest day",
                                "${cur?.busiestDay ?: "—"} (${cur?.busiestDayCount ?: 0} flights)",
                            )
                            AdbDetailRow("Current streak", "${cur?.currentStreak ?: 0} days")
                            AdbDetailRow("Longest streak", "${cur?.longestStreak ?: 0} days")
                            val op = s.leadingOperator?.operator ?: s.leadingOperator?.label
                            if (!op.isNullOrBlank()) {
                                AdbDetailRow(
                                    "Top operator",
                                    "$op · ${s.leadingOperator?.c ?: 0}",
                                )
                            }
                            val ac = s.leadingAircraft?.registration ?: s.leadingAircraft?.label
                            if (!ac.isNullOrBlank()) {
                                AdbDetailRow(
                                    "Top aircraft",
                                    "$ac · ${s.leadingAircraft?.c ?: 0}",
                                )
                            }
                        }
                    }
                    item {
                        AdbCard {
                            Text("Top operators", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            operators.forEachIndexed { idx, row ->
                                AdbRankedRow(rank = idx + 1, title = chartLabel(row), count = row.c ?: 0)
                            }
                            if (operators.isEmpty()) {
                                Text("No operator data in this range.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item {
                        AdbCard {
                            Text("Aircraft types", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            types.forEachIndexed { idx, row ->
                                AdbRankedRow(rank = idx + 1, title = chartLabel(row), count = row.c ?: 0)
                            }
                            if (types.isEmpty()) {
                                Text("No type data in this range.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}