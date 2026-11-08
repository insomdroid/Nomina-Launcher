package com.insomdroid.nomina.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
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

/** Geist Mono is reserved for Smartspace technical information. */
fun launcherTechnicalFontFamily(): FontFamily = GeistMonoFontFamily

/** Doto Rounded is reserved for numeric accents in the Smartspace clock and battery. */
fun launcherDisplayFontFamily(): FontFamily = DotoDisplayFontFamily

/** The launcher uses Geist for general interface text. */
fun launcherFontFamily(key: String = LauncherSettings.FONT_DEFAULT): FontFamily = GeistFontFamily

/**
 * Merge a local style over the theme's text style so the default Geist family remains in effect.
 * Explicit style fields such as size and weight still take precedence.
 */
@Composable
fun themedTextStyle(style: TextStyle): TextStyle = LocalTextStyle.current.merge(style)
