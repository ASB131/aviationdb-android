package uk.aviationdb.android.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.json.JSONArray
import uk.aviationdb.android.data.api.StatsChartRow
import uk.aviationdb.android.tracking.MediaUrls
import uk.aviationdb.android.ui.components.AdbRankedRow
import uk.aviationdb.android.ui.util.brandAccentColor

fun statsMediaImageUrl(media: Map<String, Any?>?): String? {
    if (media == null) return null
    val order = listOf(
        "livery",
        "tail",
        "tail_3d",
        "icon",
        "icon_dark",
        "banner_transparent",
        "banner",
        "white",
    )
    for (key in order) {
        val url = MediaUrls.absolute(media[key] as? String)
        if (url != null) return url
    }
    return null
}

fun statsBrandColor(media: Map<String, Any?>?): Color? {
    if (media == null) return null
    (media["brand_accent"] as? String)?.let { return brandAccentColor(it) }
    when (val raw = media["brand_colors"]) {
        is String -> return brandAccentColor(raw)
        is List<*> -> raw.firstOrNull()?.toString()?.let { return brandAccentColor(it) }
    }
    return null
}

@Composable
fun StatsBrandedRankedRow(
    rank: Int,
    title: String,
    count: Int,
    media: Map<String, Any?>?,
    modifier: Modifier = Modifier,
) {
    val accent = statsBrandColor(media)
    val imageUrl = statsMediaImageUrl(media)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (accent != null) {
                    Modifier.background(accent.copy(alpha = 0.12f))
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            "$rank.",
            style = MaterialTheme.typography.labelMedium,
            color = accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = accent ?: Color.Unspecified,
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = accent ?: MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun StatsBrandedRankedRow(
    rank: Int,
    row: StatsChartRow,
    title: String,
    modifier: Modifier = Modifier,
) {
    StatsBrandedRankedRow(rank, title, row.c ?: 0, row.media, modifier)
}