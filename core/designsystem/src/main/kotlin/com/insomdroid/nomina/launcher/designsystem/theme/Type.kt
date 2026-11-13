package com.insomdroid.nomina.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val LauncherTypography = Typography()

/** This typography with every style in [family]: the user's font for everything the launcher draws
 *  itself; a text style that names no family inherits it through the theme. */
fun Typography.withFontFamily(family: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

/**
 * Clock + app-label text styles per the three home styles in the design handoff.
 * Kept separate from the Material [Typography] slots since they are launcher-specific.
 */
object LauncherTextStyles {
    val clockSelkea = TextStyle(fontSize = 54.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp)
    val clockTietokeskus = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.5).sp)
    val clockIlmaisuvoimainen = TextStyle(fontSize = 78.sp, fontWeight = FontWeight.Bold, letterSpacing = (-3).sp)
    val appLabel = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal)
    val calculationExpression = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal)
    val calculationResult = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)

    val settingsEyebrow = TextStyle(fontSize = 9.sp, letterSpacing = 1.4.sp)
    val settingsTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
    val settingsRowLabel = TextStyle(fontSize = 16.sp)
    val settingsDescription = TextStyle(fontSize = 12.5.sp)
    val settingsSupportingText = TextStyle(fontSize = 12.sp)
    val settingsGroupLabel = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
    val expressiveSectionTitle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val expressiveActionLabel = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
    val expressiveActionDescription = TextStyle(fontSize = 12.sp)
}
