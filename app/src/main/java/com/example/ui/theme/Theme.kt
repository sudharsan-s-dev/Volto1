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
    primary = VoltoGreenLight,
    onPrimary = Color(0xFF052E16),
    primaryContainer = VoltoGreenDark,
    onPrimaryContainer = VoltoGreenContainer,
    secondary = VoltoYellow,
    onSecondary = Color(0xFF422006),
    secondaryContainer = VoltoYellowDark,
    onSecondaryContainer = VoltoYellowContainer,
    tertiary = VoltoYellow,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    outline = Slate600,
    error = VoltoRed
)

private val LightColorScheme = lightColorScheme(
    primary = VoltoGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = VoltoGreenContainer,
    onPrimaryContainer = VoltoOnGreenContainer,
    secondary = VoltoYellowDark,
    onSecondary = Color.White,
    secondaryContainer = VoltoYellowContainer,
    onSecondaryContainer = VoltoOnYellowContainer,
    tertiary = VoltoYellow,
    background = NeutralGray50,
    onBackground = NeutralGray900,
    surface = Color.White,
    onSurface = NeutralGray900,
    surfaceVariant = NeutralGray100,
    onSurfaceVariant = NeutralGray800,
    outline = NeutralGray200,
    error = VoltoRed
)

@Composable
fun VoltoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
