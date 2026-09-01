package com.typezero.sentinel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = SentinelCyan,
    secondary = SentinelBlue,
    tertiary = SentinelGreen,
    error = SentinelRed,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkBorder,
    onPrimary = DarkBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    onBackground = DarkOnSurface
)

private val LightColors = lightColorScheme(
    primary = ColorTokens.LightPrimary,
    secondary = ColorTokens.LightSecondary,
    tertiary = ColorTokens.LightHealthy,
    error = ColorTokens.LightError
)

private object ColorTokens {
    val LightPrimary = androidx.compose.ui.graphics.Color(0xFF00658A)
    val LightSecondary = androidx.compose.ui.graphics.Color(0xFF3C6280)
    val LightHealthy = androidx.compose.ui.graphics.Color(0xFF006C4A)
    val LightError = androidx.compose.ui.graphics.Color(0xFFBA1A1A)
}

@Composable
fun SentinelTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
