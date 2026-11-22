package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.tracking.MediaUrls
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbDetailRow
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.navigation.LocalOpenMenu
import uk.aviationdb.android.ui.util.brandAccentColor

@Composable
fun AirlineDetailScreen(icao: String, onBack: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var payload by remember { mutableStateOf<Map<String, Any?>?>(null) }

    LaunchedEffect(icao) {
        loading = true
        try {
            payload = ApiProvider.service().airlineFull(icao)
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    val airline = payload?.get("airline") as? Map<String, Any?>
    val accent = brandAccentColor(airline?.get("brand_colors")?.toString())
    @Suppress("UNCHECKED_CAST")
    val media = airline?.get("media") as? Map<String, Any?>
    @Suppress("UNCHECKED_CAST")
    val logos = media?.get("logos") as? Map<String, String?>
    val logoUrl = logos?.get("icon_light") ?: logos?.get("icon_dark")

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = icao.uppercase(),
            subtitle = airline?.get("name")?.toString() ?: "Airline",
            onBackClick = onBack,
            onMenuClick = LocalOpenMenu.current,
        )
        when {
            loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            airline != null -> {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent?.copy(alpha = 0.2f) ?: MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        logoUrl?.let {
                            AsyncImage(
                                model = MediaUrls.absolute(it),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                airline["name"]?.toString() ?: icao,
                                style = MaterialTheme.typography.headlineSmall,
                                color = accent ?: Color.Unspecified,
                            )
                            Text(
                                "${airline["icao"] ?: "—"} / ${airline["iata"] ?: "—"}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    AdbCard {
                        Column {
                            AdbDetailRow("Country", airline["country"]?.toString() ?: "—")
                            AdbDetailRow("Region", airline["region"]?.toString() ?: "—")
                            AdbDetailRow("Status", airline["status"]?.toString() ?: "—")
                            airline["website"]?.toString()?.takeIf { it.isNotBlank() }?.let {
                                AdbDetailRow("Website", it)
                            }
                            airline["headquarters"]?.toString()?.takeIf { it.isNotBlank() }?.let {
                                AdbDetailRow("Headquarters", it)
                            }
                        }
                    }

                    airline["about"]?.toString()?.takeIf { it.isNotBlank() }?.let { about ->
                        AdbCard {
                            Text("About", style = MaterialTheme.typography.titleMedium)
                            Text(about, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}