package com.insomdroid.nomina.ui

import androidx.compose.runtime.Composable
import com.insomdroid.nomina.designsystem.theme.LauncherTheme
import com.insomdroid.nomina.ui.expressive.ExpressiveTheme

/** Apply the fixed Geist interface font to every launcher activity. */
@Composable
fun LauncherAppTheme(content: @Composable () -> Unit) {
    LauncherTheme(
        fontFamily = launcherFontFamily(),
        content = {
            ExpressiveTheme(content = content)
        },
    )
}
