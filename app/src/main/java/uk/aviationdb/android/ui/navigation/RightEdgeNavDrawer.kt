package uk.aviationdb.android.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

private val edgeSwipeWidth = 48.dp
private val drawerWidth = 300.dp

@Composable
fun RightEdgeNavDrawer(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(edgeSwipeWidth)
                .zIndex(1f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount < -12f) {
                            onOpenChange(true)
                        }
                    }
                },
        )

        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(180)) + slideInHorizontally(
                animationSpec = tween(220),
                initialOffsetX = { fullWidth -> fullWidth },
            ),
            exit = fadeOut(tween(160)) + slideOutHorizontally(
                animationSpec = tween(200),
                targetOffsetX = { fullWidth -> fullWidth },
            ),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { onOpenChange(false) },
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .widthIn(min = drawerWidth, max = (LocalConfiguration.current.screenWidthDp * 0.88f).dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                if (dragAmount > 16f) onOpenChange(false)
                            }
                        },
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp,
                ) {
                    Column(modifier = Modifier.fillMaxHeight()) {
                        drawerContent()
                    }
                }
            }
        }
    }
}