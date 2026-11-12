package com.insomdroid.nomina.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.insomdroid.nomina.ui.expressive.ExpressiveRadius

val LocalTonalWidgets = staticCompositionLocalOf { false }

@Composable
internal fun widgetContentColor(): Color =
    if (LocalTonalWidgets.current) MaterialTheme.colorScheme.onSurface else Color.White

/**
 * Nomina's built-in widgets sit directly on the launcher wallpaper.
 * Keep this shared color transparent so every built-in widget uses the same treatment.
 */
@Composable
internal fun widgetSurfaceColor(): Color = Color.Transparent

@Composable
internal fun WidgetSurface(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ExpressiveRadius.card),
        color = Color.Transparent,
        contentColor = widgetContentColor(),
    ) {
        Box(content = content)
    }
}
