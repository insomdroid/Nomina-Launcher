package com.insomdroid.nomina.ui.expressive

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Warm-orange accent shared by the settings page and the drawer search results. */
val Accent = Color(0xFFD71920)

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
    /** The accent as a FOREGROUND on [surfaceHi] (titles, result text, icon tints): [Accent] itself on the system
     *  palettes, pushed toward the text on a chosen drawer colour whose cards it would vanish on (see the drawer's
     *  palette). Accent-filled surfaces (a selected tab, a button) keep [Accent] with their own white text. */
    val accent: Color = Accent,
)

val DarkExpressivePalette = ExpressivePalette(
    bg = Color(0xFF090909), surfaceHi = Color(0xFF171717), text = Color(0xFFF1F1F1),
    dim = Color(0xFFAAAAAA), faint = Color(0xFF707070), trackOff = Color(0xFF3A3A3A),
    thumbOff = Color(0xFFBDBDBD), btn = Color.White.copy(alpha = 0.06f), shadow = 0.dp,
)

val LightExpressivePalette = ExpressivePalette(
    bg = Color(0xFFF4F4F4), surfaceHi = Color(0xFFFFFFFF), text = Color(0xFF171717),
    dim = Color(0xFF5D5D5D), faint = Color(0xFF8A8A8A), trackOff = Color(0xFFD0D0D0),
    thumbOff = Color(0xFFFFFFFF), btn = Color.Black.copy(alpha = 0.04f), shadow = 0.dp,
)

val LocalExpressivePalette = staticCompositionLocalOf { LightExpressivePalette }

/** Provides the dark/light expressive palette by system theme. */
@Composable
fun ExpressiveTheme(content: @Composable () -> Unit) {
    val palette = if (isSystemInDarkTheme()) DarkExpressivePalette else LightExpressivePalette
    CompositionLocalProvider(LocalExpressivePalette provides palette, content = content)
}
