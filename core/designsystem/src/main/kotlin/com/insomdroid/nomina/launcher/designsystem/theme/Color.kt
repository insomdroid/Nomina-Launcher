package com.insomdroid.nomina.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Monochrome palette aligned with the Nothing-inspired design system.
 * Signal red is reserved for semantic errors and critical status indicators.
 */
internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1C1C1C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDEDED),
    onPrimaryContainer = Color(0xFF1C1C1C),
    secondary = Color(0xFF585A5A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDEDED),
    onSecondaryContainer = Color(0xFF1C1C1C),
    tertiary = Color(0xFF585A5A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDEDED),
    onTertiaryContainer = Color(0xFF1C1C1C),
    error = Color(0xFFC2141C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCE8E8),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2F2F2),
    onBackground = Color(0xFF1C1C1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF585A5A),
    outline = Color(0xFF9A9A9A),
    outlineVariant = Color(0xFFE5E7EB),
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEDEDED),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF171717),
    onPrimaryContainer = Color(0xFFEDEDED),
    secondary = Color(0xFF8C8C8C),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF171717),
    onSecondaryContainer = Color(0xFFEDEDED),
    tertiary = Color(0xFF8C8C8C),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF171717),
    onTertiaryContainer = Color(0xFFEDEDED),
    error = Color(0xFFFF4438),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF3A1113),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF0E0E0E),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF171717),
    onSurfaceVariant = Color(0xFF8C8C8C),
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF222222),
)
