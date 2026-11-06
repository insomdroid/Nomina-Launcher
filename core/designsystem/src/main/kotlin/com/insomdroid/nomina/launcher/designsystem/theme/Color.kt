package com.insomdroid.nomina.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Monochrome system surfaces; red is reserved for errors and critical status signals. */
internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF171717),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF171717),
    secondary = Color(0xFF555555),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E5E5),
    onSecondaryContainer = Color(0xFF191919),
    tertiary = Color(0xFF666666),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8E8E8),
    onTertiaryContainer = Color(0xFF202020),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF171717),
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFE5E5E5),
    onSurfaceVariant = Color(0xFF555555),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFFCCCCCC),
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF101010),
    primaryContainer = Color(0xFF303030),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF202020),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFE5E5E5),
    tertiary = Color(0xFFAAAAAA),
    onTertiary = Color(0xFF202020),
    tertiaryContainer = Color(0xFF363636),
    onTertiaryContainer = Color(0xFFE8E8E8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101010),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF191919),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFB8B8B8),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFF303030),
)
