package uk.aviationdb.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = Stripe,
    secondary = Navy,
    onSecondary = Color.White,
    background = Canvas,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Stripe,
    onSurfaceVariant = TextMuted,
    outline = Border,
    error = Danger,
    tertiary = Success,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6BA3E0),
    onPrimary = Navy,
    background = Color(0xFF121820),
    surface = Navy,
    onSurface = Color(0xFFE8ECF1),
    onSurfaceVariant = Color(0xFF9AA5B4),
    outline = NavyElevated,
)

@Composable
fun AviationDbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = AviationTypography,
        content = content,
    )
}