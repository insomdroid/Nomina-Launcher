package com.insomdroid.nomina.ui.component

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Automatic app-label colour follows Android's system/Monet on-surface colour and switches
 * with the device's light/dark theme. Older Android versions use a monochrome fallback.
 */
@Composable
fun systemLabelColor(): Color {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    return remember(context, darkTheme) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context).onSurface
                else dynamicLightColorScheme(context).onSurface
            darkTheme -> Color.White
            else -> Color(0xFF202124)
        }
    }
}
