package uk.aviationdb.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import uk.aviationdb.android.data.api.StatsChartRow

@Composable
fun SimpleBarChart(
    title: String,
    rows: List<StatsChartRow>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    maxBars: Int = 24,
    labelTransform: (StatsChartRow) -> String = { row ->
        row.label ?: row.t?.take(10) ?: row.operator ?: "?"
    },
) {
    val slice = remember(rows, maxBars) {
        rows.filter { (it.c ?: 0) > 0 }.take(maxBars)
    }
    if (slice.isEmpty()) {
        Text("No data in this range.", style = MaterialTheme.typography.bodySmall, modifier = modifier.padding(8.dp))
        return
    }
    val max = slice.maxOf { it.c ?: 0 }.coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .padding(top = 8.dp),
        ) {
            val barCount = slice.size.coerceAtLeast(1)
            val gap = size.width * 0.02f
            val barWidth = (size.width - gap * (barCount + 1)) / barCount
            slice.forEachIndexed { index, row ->
                val value = (row.c ?: 0).toFloat()
                val barHeight = (value / max) * size.height * 0.92f
                val left = gap + index * (barWidth + gap)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
        }
    }
}