package com.insomdroid.nomina.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.insomdroid.nomina.model.CapsuleGlyphs

/** Displays the original rounded-dot bitmap from the Vibe-Nothing icon library. */
@Composable
fun CapsuleGlyph(
    glyph: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    contentDescription: String? = null,
) {
    val definition = CapsuleGlyphs.find(glyph)
    Canvas(
        modifier = if (contentDescription == null) modifier else modifier.semantics {
            this.contentDescription = contentDescription
        },
    ) {
        val side = size.minDimension
        if (side <= 0f) return@Canvas
        val columns = definition.rows.maxOfOrNull { it.length } ?: 9
        val cell = side / maxOf(columns, definition.rows.size).toFloat()
        val left = (size.width - columns * cell) / 2f
        val top = (size.height - definition.rows.size * cell) / 2f
        val radius = cell * 0.36f
        definition.rows.forEachIndexed { row, pattern ->
            pattern.forEachIndexed { column, value ->
                if (value == '#') {
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(left + (column + 0.5f) * cell, top + (row + 0.5f) * cell),
                    )
                }
            }
        }
    }
}
