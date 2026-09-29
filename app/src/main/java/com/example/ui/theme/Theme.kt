package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonGreen,
    tertiary = NeonPurple,
    background = DarkBg,
    surface = CardBg,
    onPrimary = DarkBg,
    onSecondary = DarkBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun SmartPresensiKioskTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
