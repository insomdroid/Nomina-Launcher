package com.insomdroid.nomina.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.insomdroid.nomina.model.CapsuleGlyphs

/** A small, monochrome rounded-dot pixel glyph used by the Nomina mini-dock capsule. */
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
        val cell = side / 7f
        val dot = cell * 0.78f
        val inset = (cell - dot) / 2f
        definition.rows.forEachIndexed { row, pattern ->
            pattern.forEachIndexed { column, value ->
                if (value == '1') {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(column * cell + inset, row * cell + inset),
                        size = androidx.compose.ui.geometry.Size(dot, dot),
                        cornerRadius = CornerRadius(dot * 0.38f),
                    )
                }
            }
        }
    }
}
