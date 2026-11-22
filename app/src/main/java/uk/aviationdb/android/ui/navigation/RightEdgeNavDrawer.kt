package uk.aviationdb.android.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val edgeSwipeWidth = 40.dp
private val drawerWidth = 300.dp

@Composable
fun RightEdgeNavDrawer(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val drawerPx = with(density) { drawerWidth.toPx() }
    val offsetX by animateFloatAsState(
        targetValue = if (open) 0f else drawerPx,
        animationSpec = tween(durationMillis = 220),
        label = "drawerOffset",
    )
    val showScrim = offsetX < drawerPx * 0.95f

    Box(modifier = modifier.fillMaxSize()) {
        content()

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(edgeSwipeWidth)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, amount ->
                        if (amount < -6) onOpenChange(true)
                    }
                },
        )

        if (showScrim) {
            val scrimAlpha = ((1f - offsetX / drawerPx) * 0.5f).coerceIn(0f, 0.5f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onOpenChange(false) }
                    .pointerInput(open) {
                        detectHorizontalDragGestures { _, amount ->
                            if (amount > 10) onOpenChange(false)
                        }
                    },
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .widthIn(min = drawerWidth, max = (LocalConfiguration.current.screenWidthDp * 0.88f).dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, amount ->
                        if (amount > 10) onOpenChange(false)
                    }
                },
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                drawerContent()
            }
        }
    }
}