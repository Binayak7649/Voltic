package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VoltEliteColorScheme = darkColorScheme(
    primary = VoltGreen,
    onPrimary = VoltDarkBg,
    primaryContainer = VoltGreenDark,
    onPrimaryContainer = VoltGreen,
    secondary = VoltCyan,
    onSecondary = VoltDarkBg,
    secondaryContainer = VoltCardElevated,
    onSecondaryContainer = VoltCyan,
    tertiary = VoltPurple,
    onTertiary = VoltDarkBg,
    background = VoltDarkBg,
    onBackground = VoltTextPrimary,
    surface = VoltSurface,
    onSurface = VoltTextPrimary,
    surfaceVariant = VoltCard,
    onSurfaceVariant = VoltTextSecondary,
    outline = VoltCardBorder,
    error = VoltRed,
    onError = VoltTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Always enforce VoltElite luxury dark styling
    content: @Composable () -> Unit
) {
    val colorScheme = VoltEliteColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VoltDarkBg.toArgb()
            window.navigationBarColor = VoltDarkBg.toArgb()
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
