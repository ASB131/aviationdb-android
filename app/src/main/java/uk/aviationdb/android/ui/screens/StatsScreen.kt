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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.api.StatsChartRow
import uk.aviationdb.android.data.api.StatsKpisResponse
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
    row.label ?: row.registration ?: row.operator ?: row.t?.take(12) ?: "?"

private fun dowLabel(row: StatsChartRow): String {
    val raw = row.label ?: return "?"
    val n = raw.toIntOrNull() ?: return raw
    return when (n) {
        0 -> "Sun"
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        else -> raw
    }
}

private fun wakeLabel(code: String?): String = when (code?.uppercase()) {
    "H" -> "Heavy"
    "L" -> "Light"
    "M" -> "Medium"
    "L/M" -> "Light / Medium"
    "M/H" -> "Medium / Heavy"
    else -> code ?: "—"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(sessionStore: SessionStore) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var range by remember { mutableStateOf("today") }
    var summary by remember { mutableStateOf<StatsSummaryResponse?>(null) }
    var kpis by remember { mutableStateOf<StatsKpisResponse?>(null) }
    var operators by remember { mutableStateOf<List<StatsChartRow>>(emptyList()) }
    var types by remember { mutableStateOf<List<StatsChartRow>>(emptyList()) }
    var aircraft by remember { mutableStateOf<List<StatsChartRow>>(emptyList()) }
    var callsigns by remember { mutableStateOf<List<StatsChartRow>>(emptyList()) }
    var airports by remember { mutableStateOf<List<StatsChartRow>>(emptyList()) }
    var routes by remember { mutableStateOf<List<StatsChartRow>>(emptyList()) }
    var hourly by remember { mutableStateOf<List<StatsChartRow>>(emptyList()) }
    var dow by remember { mutableStateOf<List<StatsChartRow>>(emptyList()) }
    var wake by remember { mutableStateOf<List<StatsChartRow>>(emptyList()) }
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
            coroutineScope {
                val api = ApiProvider.service()
                val s = async { api.statsSummary(range) }
                val k = async { api.statsKpis(range) }
                val op = async { api.statsCharts("operators", range) }
                val ty = async { api.statsCharts("types", range) }
                val ac = async { api.statsCharts("aircraft", range) }
                val cs = async { api.statsCharts("callsigns", range) }
                val ap = async { api.statsCharts("airports", range) }
                val rt = async { api.statsCharts("routes", range) }
                val hr = async { api.statsCharts("hourly", range) }
                val dw = async { api.statsCharts("dow", range) }
                val wk = async { api.statsCharts("wake", range) }
                summary = s.await()
                kpis = k.await()
                operators = op.await().rows.orEmpty()
                types = ty.await().rows.orEmpty()
                aircraft = ac.await().rows.orEmpty()
                callsigns = cs.await().rows.orEmpty()
                airports = ap.await().rows.orEmpty()
                routes = rt.await().rows.orEmpty()
                hourly = hr.await().rows.orEmpty()
                dow = dw.await().rows.orEmpty()
                wake = wk.await().rows.orEmpty()
            }
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
                val kpi = kpis
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
                    if (kpi != null) {
                        item {
                            AdbCard {
                                Text("Activity in range", style = MaterialTheme.typography.titleMedium)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                AdbDetailRow("Avg per active day", "${kpi.avgPerDay ?: 0}")
                                AdbDetailRow("Avg per hour", "${kpi.avgPerHour ?: 0}")
                                AdbDetailRow(
                                    "With full route",
                                    "${kpi.withRoute ?: 0} (${kpi.routePct ?: 0}%)",
                                )
                                AdbDetailRow("Unique airports", nf.format(kpi.uniqueAirports ?: 0))
                                AdbDetailRow(
                                    "One-time visitors",
                                    nf.format(kpi.singleVisitRegistrations ?: 0),
                                )
                                kpi.peakHour?.let { peak ->
                                    AdbDetailRow(
                                        "Busiest hour",
                                        "${String.format(Locale.UK, "%02d:00", peak)} · ${kpi.peakHourCount ?: 0} sightings",
                                    )
                                }
                            }
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
                                AdbDetailRow("Top operator", "$op · ${s.leadingOperator?.c ?: 0}")
                            }
                            val ac = s.leadingAircraft?.registration ?: s.leadingAircraft?.label
                            if (!ac.isNullOrBlank()) {
                                AdbDetailRow("Top aircraft", "$ac · ${s.leadingAircraft?.c ?: 0}")
                            }
                        }
                    }
                    rankedListCard("Top operators", operators)
                    rankedListCard("Aircraft types", types)
                    rankedListCard("Top registrations", aircraft)
                    rankedListCard("Top callsigns", callsigns)
                    rankedListCard("Busiest airports", airports)
                    rankedListCard("Top routes", routes)
                    rankedListCard("By hour (local)", hourly)
                    rankedListCard("By day of week", dow) { dowLabel(it) }
                    rankedListCard("Wake turbulence", wake) { wakeLabel(it.label) }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.rankedListCard(
    title: String,
    rows: List<StatsChartRow>,
    label: (StatsChartRow) -> String = { chartLabel(it) },
) {
    item {
        AdbCard {
            Text(title, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            rows.forEachIndexed { idx, row ->
                AdbRankedRow(rank = idx + 1, title = label(row), count = row.c ?: 0)
            }
            if (rows.isEmpty()) {
                Text("No data in this range.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}