@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.PlanespottersPhotos
import uk.aviationdb.android.data.api.MeAircraftResponse
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbDetailRow
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.navigation.LocalOpenMenu
import uk.aviationdb.android.tracking.MediaUrls

@Composable
fun AircraftDetailScreen(registration: String, onBack: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<MeAircraftResponse?>(null) }
    var galleryUrls by remember(registration) { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(registration) {
        loading = true
        error = null
        try {
            data = ApiProvider.service().meAircraft(registration, limit = 25)
        } catch (e: Exception) {
            error = e.message ?: "Could not load aircraft"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(registration, data) {
        val full = data?.aircraftFull
        @Suppress("UNCHECKED_CAST")
        val media = full?.get("media") as? Map<String, Any?>
        val livery = MediaUrls.absolute(media?.get("livery_path") as? String)
        val white = MediaUrls.absolute(media?.get("white_template_path") as? String)
        val spotter = PlanespottersPhotos.photoUrls(registration, max = 12)
        galleryUrls = buildList {
            livery?.let { add(it) }
            white?.let { if (it !in this) add(it) }
            spotter.forEach { if (it !in this) add(it) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = registration.uppercase(),
            subtitle = data?.aircraft?.aircraftModel ?: "Aircraft profile",
            onBackClick = onBack,
            onMenuClick = LocalOpenMenu.current,
        )

        when {
            loading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            data != null -> {
                val d = data!!
                val a = d.aircraft
                @Suppress("UNCHECKED_CAST")
                val full = d.aircraftFull as? Map<String, Any?>
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (galleryUrls.isNotEmpty()) {
                        item {
                            AircraftPhotoGallery(galleryUrls)
                        }
                    }

                    item {
                        AdbCard {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(a?.registration ?: registration, style = MaterialTheme.typography.titleLarge)
                                AdbDetailRow("Model", a?.aircraftModel ?: full?.get("manufacturer_and_model")?.toString() ?: "—")
                                AdbDetailRow("Type", a?.aircraftType ?: full?.get("model_icao")?.toString() ?: "—")
                                AdbDetailRow("Operator", a?.operator ?: full?.get("operator")?.toString() ?: "—")
                                AdbDetailRow("ICAO24", a?.icao24 ?: full?.get("icao24")?.toString() ?: "—")
                                AdbDetailRow("Serial", full?.get("serial_number")?.toString() ?: "—")
                                AdbDetailRow("Year built", full?.get("year_built")?.toString() ?: "—")
                            }
                        }
                    }

                    d.yourStats?.let { st ->
                        item {
                            AdbCard {
                                Text("Your logbook (${d.historyDays ?: 0} days)", style = MaterialTheme.typography.titleMedium)
                                AdbDetailRow("Sightings", (st.totalSightings ?: 0).toString())
                                AdbDetailRow("Callsigns", (st.uniqueCallsigns ?: 0).toString())
                                AdbDetailRow("First seen", st.firstSeen ?: "—")
                                AdbDetailRow("Last seen", st.lastSeen ?: "—")
                            }
                        }
                    }

                    val routes = d.routes.orEmpty()
                    if (routes.isNotEmpty()) {
                        item {
                            AdbCard {
                                Text("Routes you've logged", style = MaterialTheme.typography.titleMedium)
                                routes.forEach { r ->
                                    val line = listOfNotNull(r.routeFrom, r.routeTo).joinToString(" → ")
                                    Text(
                                        "$line · ${r.timesSeen ?: 0}× · last ${r.lastSeen ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }
                        }
                    }

                    val sightings = d.sightings.orEmpty()
                    if (sightings.isNotEmpty()) {
                        item {
                            Text("Recent sightings", style = MaterialTheme.typography.titleMedium)
                        }
                        items(sightings.take(15), key = { it.seenAt ?: it.callsign ?: it.hashCode() }) { s ->
                            AdbCard {
                                Column {
                                    Text(
                                        listOfNotNull(s.callsign, s.seenAt).joinToString(" · "),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    val route = listOfNotNull(s.routeFrom, s.routeTo).joinToString(" → ")
                                    if (route.isNotBlank()) Text(route, style = MaterialTheme.typography.bodySmall)
                                    s.operator?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AircraftPhotoGallery(urls: List<String>) {
    val pagerState = rememberPagerState(pageCount = { urls.size })
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
        ) { page ->
            AsyncImage(
                model = urls[page],
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit,
            )
        }
        if (urls.size > 1) {
            Text(
                "Swipe for livery and spotting photos · ${pagerState.currentPage + 1} of ${urls.size}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}