package com.insomdroid.nomina.ui

import androidx.compose.runtime.Composable
import com.insomdroid.nomina.designsystem.theme.LauncherTheme
import com.insomdroid.nomina.model.LauncherSettings

/** Apply the fixed Geist interface font to every launcher activity. */
@Composable
fun LauncherAppTheme(content: @Composable () -> Unit) {
    LauncherTheme(
        fontFamily = launcherFontFamily(LauncherSettings.FONT_DEFAULT),
        content = content,
    )
}
