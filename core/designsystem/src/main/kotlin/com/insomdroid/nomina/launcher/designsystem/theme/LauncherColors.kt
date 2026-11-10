package com.insomdroid.nomina.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Launcher-specific colors outside Material 3. The battery indicator stays monochrome,
 * with red reserved for a critically low charge level.
 */
@Immutable
data class LauncherColors(
    val batteryNormal: Color,
    val batteryLow: Color,
    val dockScrim: Color,
    val pageIndicatorActive: Color,
    val pageIndicatorInactive: Color,
) {
    fun batteryColor(percent: Int): Color =
        if (percent.coerceIn(0, 100) < 20) batteryLow else batteryNormal
}

internal val LightLauncherColors = LauncherColors(
    batteryNormal = Color(0xFF777777),
    batteryLow = Color(0xFFD71921),
    dockScrim = Color(0x2EFFFFFF),
    pageIndicatorActive = Color(0xFFFFFFFF),
    pageIndicatorInactive = Color(0x66FFFFFF),
)

internal val DarkLauncherColors = LauncherColors(
    batteryNormal = Color(0xFFBDBDBD),
    batteryLow = Color(0xFFD71921),
    dockScrim = Color(0x12FFFFFF),
    pageIndicatorActive = Color(0xFFFFFFFF),
    pageIndicatorInactive = Color(0x66FFFFFF),
)

val LocalLauncherColors = staticCompositionLocalOf<LauncherColors> {
    error("LauncherColors not provided. Wrap content in LauncherTheme.")
}
