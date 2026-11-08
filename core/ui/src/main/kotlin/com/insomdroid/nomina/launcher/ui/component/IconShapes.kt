package com.insomdroid.nomina.ui.component

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.insomdroid.nomina.model.LauncherSettings

/**
 * Returns the selected icon mask. The system option uses Android's public adaptive-icon mask so
 * the shape follows the device's actual launcher mask instead of assuming one universal default.
 */
fun launcherIconShape(key: String): Shape = when (key) {
    LauncherSettings.ICON_SHAPE_CIRCLE -> CircleShape
    LauncherSettings.ICON_SHAPE_SQUIRCLE -> RoundedCornerShape(34)
    LauncherSettings.ICON_SHAPE_ROUNDED_SQUARE -> RoundedCornerShape(22)
    LauncherSettings.ICON_SHAPE_SQUARE -> RoundedCornerShape(2)
    LauncherSettings.ICON_SHAPE_TEARDROP -> GenericShape { size, _ ->
        moveTo(size.width * 0.5f, 0f)
        cubicTo(size.width * 0.82f, size.height * 0.18f, size.width, size.height * 0.43f, size.width, size.height * 0.62f)
        cubicTo(size.width, size.height * 0.84f, size.width * 0.78f, size.height, size.width * 0.5f, size.height)
        cubicTo(size.width * 0.22f, size.height, 0f, size.height * 0.84f, 0f, size.height * 0.62f)
        cubicTo(0f, size.height * 0.43f, size.width * 0.18f, size.height * 0.18f, size.width * 0.5f, 0f)
        close()
    }
    LauncherSettings.ICON_SHAPE_HEXAGON -> polygonShape(6, 0.5f)
    LauncherSettings.ICON_SHAPE_OCTAGON -> polygonShape(8, 0.5f)
    LauncherSettings.ICON_SHAPE_FLOWER -> GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = minOf(size.width, size.height) * 0.48f
        val inner = radius * 0.76f
        for (i in 0 until 16) {
            val angle = Math.PI * i / 8.0 - Math.PI / 2.0
            val r = if (i % 2 == 0) radius else inner
            val x = cx + kotlin.math.cos(angle).toFloat() * r
            val y = cy + kotlin.math.sin(angle).toFloat() * r
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    LauncherSettings.ICON_SHAPE_CAT -> GenericShape { size, _ ->
        moveTo(size.width * 0.12f, size.height * 0.34f)
        lineTo(size.width * 0.10f, size.height * 0.04f)
        quadraticTo(size.width * 0.09f, size.height * -0.01f, size.width * 0.15f, size.height * 0.04f)
        lineTo(size.width * 0.38f, size.height * 0.22f)
        cubicTo(size.width * 0.46f, size.height * 0.19f, size.width * 0.54f, size.height * 0.19f, size.width * 0.62f, size.height * 0.22f)
        lineTo(size.width * 0.85f, size.height * 0.04f)
        quadraticTo(size.width * 0.91f, size.height * -0.01f, size.width * 0.90f, size.height * 0.04f)
        lineTo(size.width * 0.88f, size.height * 0.34f)
        cubicTo(size.width * 0.96f, size.height * 0.48f, size.width, size.height * 0.64f, size.width * 0.94f, size.height * 0.78f)
        cubicTo(size.width * 0.87f, size.height * 0.94f, size.width * 0.68f, size.height, size.width * 0.5f, size.height)
        cubicTo(size.width * 0.32f, size.height, size.width * 0.13f, size.height * 0.94f, size.width * 0.06f, size.height * 0.78f)
        cubicTo(0f, size.height * 0.64f, size.width * 0.04f, size.height * 0.48f, size.width * 0.12f, size.height * 0.34f)
        close()
    }
    else -> SystemAdaptiveIconShape
}

private fun polygonShape(sides: Int, radiusFactor: Float): Shape = GenericShape { size, _ ->
    val radius = minOf(size.width, size.height) * radiusFactor
    val cx = size.width / 2f
    val cy = size.height / 2f
    for (i in 0 until sides) {
        val angle = Math.PI * 2.0 * i / sides - Math.PI / 2.0
        val x = cx + kotlin.math.cos(angle).toFloat() * radius
        val y = cy + kotlin.math.sin(angle).toFloat() * radius
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private object SystemAdaptiveIconShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val width = size.width.toInt().coerceAtLeast(1)
        val height = size.height.toInt().coerceAtLeast(1)
        val drawable = AdaptiveIconDrawable(ColorDrawable(android.graphics.Color.BLACK), null)
        drawable.setBounds(0, 0, width, height)
        val path: Path = drawable.iconMask.asComposePath()
        return Outline.Generic(path)
    }
}
