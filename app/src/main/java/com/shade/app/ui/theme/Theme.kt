package com.shade.app.ui.theme

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
    primary = ElectricPurple,
    secondary = SoftPurple,
    tertiary = AccentPurple,
    background = DarkBlack,
    surface = DeepPurple,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = DeepPurple,
    onPrimaryContainer = AccentPurple
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricPurple,
    secondary = SoftPurple,
    tertiary = AccentPurple,
    background = Color.White,
    surface = Color(0xFFF3E5F5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkBlack,
    onSurface = DarkBlack
)

@Composable
fun ShadeTheme(
    darkTheme: Boolean = true, // Force dark theme for the "black and purple" look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
