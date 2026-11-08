package com.insomdroid.nomina.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import android.graphics.Typeface
import java.io.File
import com.insomdroid.nomina.model.LauncherSettings

private val GeistFontFamily = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

/** Geist Mono is reserved for technical labels and compact system data. */
fun launcherTechnicalFontFamily(): FontFamily = GeistFontFamily

/** The rounded-dot Doto face is reserved for numeric clock displays, not general UI text. */
fun launcherDisplayFontFamily(): FontFamily = GeistFontFamily

/** The selected family for launcher-owned text. Removed legacy choices resolve safely to Geist. */
fun launcherFontFamily(key: String, customFontPath: String = ""): FontFamily =
    if (key == LauncherSettings.FONT_CUSTOM && customFontPath.isNotBlank()) {
        runCatching { FontFamily(Typeface.createFromFile(File(customFontPath))) }.getOrDefault(GeistFontFamily)
    } else {
        GeistFontFamily
    }

/**
 * Merge a local style over the theme's text style so the user's chosen family remains in effect.
 * Explicit style fields such as size and weight still take precedence.
 */
@Composable
fun themedTextStyle(style: TextStyle): TextStyle = LocalTextStyle.current.merge(style)
