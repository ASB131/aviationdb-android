package uk.aviationdb.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import uk.aviationdb.android.data.api.ActiveFlight
import uk.aviationdb.android.data.api.Sighting
import uk.aviationdb.android.ui.theme.Border
import uk.aviationdb.android.ui.theme.Navy
import uk.aviationdb.android.ui.theme.SkyBlue
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun AdbTopBanner(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
  onMenuClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Surface(
        color = Navy,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                onBackClick != null -> {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                onMenuClick != null -> {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = if (onBackClick != null || onMenuClick != null) 0.dp else 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFC8D0DB))
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun AdbCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Border, shape)
            .then(clickMod),
    ) {
        Column(modifier = Modifier.padding(14.dp)) { content() }
    }
}

@Composable
fun FlightRowCard(
    flight: ActiveFlight,
    imageUrl: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AdbCard(onClick = onClick, modifier = modifier) {
        Row {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .padding(end = 10.dp),
                contentScale = ContentScale.Fit,
            )
            Column(modifier = Modifier.weight(1f)) {
                    val title = listOfNotNull(flight.callsign, flight.registration)
                        .joinToString(" · ")
                        .ifBlank { "Unknown" }
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    val route = listOfNotNull(flight.originIcao, flight.destIcao).joinToString(" → ")
                    if (route.isNotBlank()) {
                        Text(route, style = MaterialTheme.typography.bodyMedium)
                    }
                    val meta = buildList {
                        flight.airline?.let { add(it) }
                        flight.aircraftType?.let { add(it) }
                        if (flight.altitude != null) add("${flight.altitude} ft")
                        if (flight.speed != null) add("${flight.speed} kt")
                    }.joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall)
                    }
            }
        }
    }
}

@Composable
fun SightingRowCard(sighting: Sighting) {
    AdbCard {
        Column {
            Text(
                listOfNotNull(sighting.registration, sighting.callsign).joinToString(" · ").ifBlank { "Sighting" },
                style = MaterialTheme.typography.titleMedium,
            )
            sighting.seenAt?.let {
                Text(it, style = MaterialTheme.typography.labelSmall)
            }
            val line = buildList {
                sighting.operator?.let { add(it) }
                sighting.aircraftType?.let { add(it) }
                val route = listOfNotNull(sighting.routeFrom, sighting.routeTo).joinToString(" → ")
                if (route.isNotBlank()) add(route)
            }.joinToString(" · ")
            if (line.isNotBlank()) {
                Text(line, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    AdbCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = SkyBlue)
        }
    }
}

@Composable
fun AdbDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 2.dp),
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AdbRankedRow(
    rank: Int,
    title: String,
    subtitle: String? = null,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$rank.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = SkyBlue,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}