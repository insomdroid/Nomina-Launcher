package com.insomdroid.nomina.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.insomdroid.nomina.model.LauncherSettings

private val GeistFontFamily = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

private val GeistMonoFontFamily = FontFamily(
    Font(R.font.geistmono_regular, FontWeight.Normal),
)

private val DotoDisplayFontFamily = FontFamily(
    Font(R.font.doto_rond_wght, FontWeight.Normal),
)

/** Geist Mono is reserved for technical labels and compact system data. */
fun launcherTechnicalFontFamily(): FontFamily = GeistMonoFontFamily

/** The rounded-dot Doto face is reserved for numeric clock displays, not general UI text. */
fun launcherDisplayFontFamily(): FontFamily = DotoDisplayFontFamily

private val CondensedFontFamily = FontFamily(Font(DeviceFontFamilyName("sans-serif-condensed")))
private val CasualFontFamily = FontFamily(Font(DeviceFontFamilyName("casual")))

/** The selected family for launcher-owned text. The legacy "default" key now resolves to Geist. */
fun launcherFontFamily(key: String): FontFamily = when (key) {
    LauncherSettings.FONT_DEFAULT -> GeistFontFamily
    LauncherSettings.FONT_GEIST_MONO -> GeistMonoFontFamily
    LauncherSettings.FONT_CONDENSED -> CondensedFontFamily
    LauncherSettings.FONT_SERIF -> FontFamily.Serif
    LauncherSettings.FONT_MONOSPACE -> FontFamily.Monospace
    LauncherSettings.FONT_CASUAL -> CasualFontFamily
    LauncherSettings.FONT_CURSIVE -> FontFamily.Cursive
    else -> GeistFontFamily
}

/**
 * Merge a local style over the theme's text style so the user's chosen family remains in effect.
 * Explicit style fields such as size and weight still take precedence.
 */
@Composable
fun themedTextStyle(style: TextStyle): TextStyle = LocalTextStyle.current.merge(style)
