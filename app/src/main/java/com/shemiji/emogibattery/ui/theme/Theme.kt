package com.shemiji.emogibattery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppLightColorScheme = lightColorScheme(
    primary = AccentRed,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF1E2270),
    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = Color(0xFF004D47),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = Color(0xFFB00020),
    onError = Color.White,
    tertiary = LightTertiary,
)

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

@Composable
fun ShemijiBatteryTheme(
    appTheme: AppTheme = AppTheme.LIGHT,
    content: @Composable () -> Unit
) {
    // Force light theme
    MaterialTheme(
        colorScheme = AppLightColorScheme,
        typography = Typography,
        content = content
    )
}
