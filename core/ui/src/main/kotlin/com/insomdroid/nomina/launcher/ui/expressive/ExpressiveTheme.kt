package com.insomdroid.nomina.ui.expressive

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Nothing Red, reserved for critical or live status rather than ordinary controls. */
val Accent = Color(0xFFD71921)

data class ExpressivePalette(
    val bg: Color,
    val surfaceHi: Color,
    val text: Color,
    val dim: Color,
    val faint: Color,
    val trackOff: Color,
    val thumbOff: Color,
    val btn: Color,
    val shadow: Dp,
    /** Reserved red signal colour. Ordinary controls use text/onSurface colours instead. */
    val accent: Color = Accent,
)

val DarkExpressivePalette = ExpressivePalette(
    bg = Color(0xFF000000),
    surfaceHi = Color(0xFF0E0E0E),
    text = Color(0xFFEDEDED),
    dim = Color(0xFF8C8C8C),
    faint = Color(0xFF5A5A5A),
    trackOff = Color(0xFF222222),
    thumbOff = Color(0xFF333333),
    btn = Color(0xFF171717),
    shadow = 0.dp,
)

val LightExpressivePalette = ExpressivePalette(
    bg = Color(0xFFF2F2F2),
    surfaceHi = Color(0xFFFFFFFF),
    text = Color(0xFF1C1C1C),
    dim = Color(0xFF585A5A),
    faint = Color(0xFF9A9A9A),
    trackOff = Color(0xFFE5E7EB),
    thumbOff = Color(0xFFCFCFCF),
    btn = Color(0xFFEDEDED),
    shadow = 0.dp,
)

val LocalExpressivePalette = staticCompositionLocalOf { LightExpressivePalette }

/** Provides the shared dark/light palette by system theme. */
@Composable
fun ExpressiveTheme(content: @Composable () -> Unit) {
    val palette = if (isSystemInDarkTheme()) DarkExpressivePalette else LightExpressivePalette
    CompositionLocalProvider(LocalExpressivePalette provides palette, content = content)
}
