package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = Color.Black,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkAccent,
    secondary = DarkAccent,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = LightAccent,
    secondary = LightAccent,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary
)

// Global Theme State to allow manual toggling from Settings
enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

object MythicThemeState {
    var themeMode by mutableStateOf(ThemeMode.DARK) // Default to premium dark as requested
}

@Composable
fun MythicTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = when (MythicThemeState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
