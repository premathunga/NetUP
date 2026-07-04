package com.pingoptimizer.pro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentBlue,
    tertiary = AccentPurple,
    background = BgDeep,
    surface = BgSurface,
    surfaceVariant = BgCard,
    error = DangerRed,
    onPrimary = BgDeep,
    onSecondary = BgDeep,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Divider
)

@Composable
fun PingOptimizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // App is gaming-focused: always use the dark palette regardless of system theme
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
