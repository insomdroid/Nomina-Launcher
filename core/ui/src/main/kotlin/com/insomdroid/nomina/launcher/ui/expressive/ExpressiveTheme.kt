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
    bg = Color(0xFF101010), surfaceHi = Color(0xFF191919), text = Color(0xFFF5F5F5),
    dim = Color(0xFFA0A0A0), faint = Color(0xFF707070), trackOff = Color(0xFF303030),
    thumbOff = Color(0xFFBDBDBD), btn = Color.White.copy(alpha = 0.05f), shadow = 0.dp,
)

val LightExpressivePalette = ExpressivePalette(
    bg = Color(0xFFFFFFFF), surfaceHi = Color(0xFFF5F5F5), text = Color(0xFF171717),
    dim = Color(0xFF777777), faint = Color(0xFF999999), trackOff = Color(0xFFD0D0D0),
    thumbOff = Color(0xFFFFFFFF), btn = Color.Black.copy(alpha = 0.035f), shadow = 0.dp,
)

val LocalExpressivePalette = staticCompositionLocalOf { LightExpressivePalette }

/** Provides the dark/light expressive palette by system theme. */
@Composable
fun ExpressiveTheme(content: @Composable () -> Unit) {
    val palette = if (isSystemInDarkTheme()) DarkExpressivePalette else LightExpressivePalette
    CompositionLocalProvider(LocalExpressivePalette provides palette, content = content)
}
