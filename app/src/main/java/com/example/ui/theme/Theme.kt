package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = KioskDarkBackground,
    primaryContainer = Color(0xFF083344),
    onPrimaryContainer = CyanNeonLight,
    secondary = EmeraldSuccess,
    onSecondary = KioskDarkBackground,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = EmeraldSuccessLight,
    tertiary = AmberCheckout,
    onTertiary = KioskDarkBackground,
    background = KioskDarkBackground,
    onBackground = TextPrimary,
    surface = KioskDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = KioskCardBackground,
    onSurfaceVariant = TextSecondary,
    outline = KioskCardBorder,
    error = RoseError,
    onError = TextPrimary
)

private val LightColorScheme = DarkColorScheme // Kiosk defaults to high-contrast futuristic dark mode

@Composable
fun SmartPresensiTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
