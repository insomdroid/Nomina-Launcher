package com.insomdroid.nomina.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Launcher-specific dynamic colors that are NOT part of the Material 3 [androidx.compose.material3.ColorScheme]:
 * the dynamically colored battery indicator and the signal-quality bar colors from the design handoff.
 * Provided via [LocalLauncherColors].
 */
@Immutable
data class LauncherColors(
    val batteryHigh: Color,
    val batteryMedium: Color,
    val batteryLow: Color,
    /** Signal quality colors, index 0 (weakest) .. 4 (strongest). */
    val signal: List<Color>,
    val signalFaint: Color,
    /** Monochrome status glyphs (Bluetooth, VPN, alarm, DND, …) over the wallpaper. */
    val statusFlag: Color,
    val dockScrim: Color,
    val pageIndicatorActive: Color,
    val pageIndicatorInactive: Color,
) {
    /** Red is reserved for a genuinely low battery; normal levels stay monochrome. */
    fun batteryColor(percent: Int): Color {
        val p = percent.coerceIn(0, 100)
        return if (p < 20) lerp(batteryLow, batteryMedium, p / 20f) else batteryHigh
    }

    fun signalColor(level: Int): Color = signal[level.coerceIn(0, 4)]
}

internal val LightLauncherColors = LauncherColors(
    batteryHigh = Color(0xFFBDBDBD),
    batteryMedium = Color(0xFFBDBDBD),
    batteryLow = Color(0xFFE53935),
    signal = List(5) { Color(0xFFBDBDBD) },
    signalFaint = Color(0x73FFFFFF),
    statusFlag = Color(0xFFF5F5F5),
    dockScrim = Color(0x2EFFFFFF),
    pageIndicatorActive = Color(0xFFFFFFFF),
    pageIndicatorInactive = Color(0x66FFFFFF),
)

internal val DarkLauncherColors = LauncherColors(
    batteryHigh = Color(0xFFBDBDBD),
    batteryMedium = Color(0xFFBDBDBD),
    batteryLow = Color(0xFFE53935),
    signal = List(5) { Color(0xFFBDBDBD) },
    signalFaint = Color(0x29FFFFFF),
    statusFlag = Color(0xFFF5F5F5),
    dockScrim = Color(0x12FFFFFF),
    pageIndicatorActive = Color(0xFFFFFFFF),
    pageIndicatorInactive = Color(0x66FFFFFF),
)

val LocalLauncherColors = staticCompositionLocalOf<LauncherColors> {
    error("LauncherColors not provided. Wrap content in LauncherTheme.")
}
