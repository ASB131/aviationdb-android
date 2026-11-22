package uk.aviationdb.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Sky = Color(0xFF5BC0EB)
private val Navy = Color(0xFF0F2A3D)
private val Slate = Color(0xFF1B3A4B)

private val DarkColors = darkColorScheme(
    primary = Sky,
    onPrimary = Navy,
    background = Color(0xFF0B1620),
    surface = Slate,
    onBackground = Color(0xFFE8F1F5),
    onSurface = Color(0xFFE8F1F5),
)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Sky,
    background = Color(0xFFF4F8FB),
    surface = Color.White,
    onBackground = Navy,
    onSurface = Navy,
)

@Composable
fun AviationDbTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}