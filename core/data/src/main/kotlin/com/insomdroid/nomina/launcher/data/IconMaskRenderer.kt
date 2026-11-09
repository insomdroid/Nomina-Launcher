package com.insomdroid.nomina.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import com.insomdroid.nomina.model.LauncherSettings
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Draws legacy artwork centred on a selected backplate shape without clipping the artwork. */
internal fun applyIconMask(
    source: Bitmap,
    shape: String,
    backgroundColor: Int = FALLBACK_ICON_BACKGROUND,
): Bitmap {
    val size = source.width
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val path = iconMaskPath(shape, size)

    // The shape is the backplate only. The original artwork is scaled proportionally and
    // kept intact; it is deliberately not clipped to the backplate path.
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor })
    val inset = size * ICON_ARTWORK_INSET
    canvas.drawBitmap(
        source,
        null,
        RectF(inset, inset, size - inset, size - inset),
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
    )
    return output
}

internal const val FALLBACK_ICON_BACKGROUND: Int = 0xFF808080.toInt()
private const val ICON_ARTWORK_INSET = 0.16f

private fun iconMaskPath(shape: String, size: Int): Path {
    val s = size.toFloat()
    val path = Path()
    when (shape) {
        LauncherSettings.ICON_SHAPE_CIRCLE -> path.addCircle(s / 2f, s / 2f, s / 2f, Path.Direction.CW)
        LauncherSettings.ICON_SHAPE_SQUIRCLE -> rounded(path, s, 0.34f)
        LauncherSettings.ICON_SHAPE_ROUNDED_SQUARE -> rounded(path, s, 0.22f)
        LauncherSettings.ICON_SHAPE_SQUARE -> rounded(path, s, 0.02f)
        LauncherSettings.ICON_SHAPE_TEARDROP -> {
            path.moveTo(s * 0.5f, 0f)
            path.cubicTo(s * 0.82f, s * 0.18f, s, s * 0.43f, s, s * 0.62f)
            path.cubicTo(s, s * 0.84f, s * 0.78f, s, s * 0.5f, s)
            path.cubicTo(s * 0.22f, s, 0f, s * 0.84f, 0f, s * 0.62f)
            path.cubicTo(0f, s * 0.43f, s * 0.18f, s * 0.18f, s * 0.5f, 0f)
            path.close()
        }
        LauncherSettings.ICON_SHAPE_HEXAGON -> polygon(path, s, 6)
        LauncherSettings.ICON_SHAPE_OCTAGON -> polygon(path, s, 8)
        LauncherSettings.ICON_SHAPE_FLOWER -> {
            val radius = s * 0.48f
            val inner = radius * 0.76f
            for (i in 0 until 16) {
                val angle = Math.PI * i / 8.0 - Math.PI / 2.0
                val r = if (i % 2 == 0) radius else inner
                val x = s / 2f + cos(angle).toFloat() * r
                val y = s / 2f + sin(angle).toFloat() * r
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }
        LauncherSettings.ICON_SHAPE_CAT -> {
            path.moveTo(s * 0.12f, s * 0.34f)
            path.lineTo(s * 0.10f, s * 0.04f)
            path.quadTo(s * 0.09f, -s * 0.01f, s * 0.15f, s * 0.04f)
            path.lineTo(s * 0.38f, s * 0.22f)
            path.cubicTo(s * 0.46f, s * 0.19f, s * 0.54f, s * 0.19f, s * 0.62f, s * 0.22f)
            path.lineTo(s * 0.85f, s * 0.04f)
            path.quadTo(s * 0.91f, -s * 0.01f, s * 0.90f, s * 0.04f)
            path.lineTo(s * 0.88f, s * 0.34f)
            path.cubicTo(s * 0.96f, s * 0.48f, s, s * 0.64f, s * 0.94f, s * 0.78f)
            path.cubicTo(s * 0.87f, s * 0.94f, s * 0.68f, s, s * 0.5f, s)
            path.cubicTo(s * 0.32f, s, s * 0.13f, s * 0.94f, s * 0.06f, s * 0.78f)
            path.cubicTo(0f, s * 0.64f, s * 0.04f, s * 0.48f, s * 0.12f, s * 0.34f)
            path.close()
        }
        else -> {
            val adaptive = AdaptiveIconDrawable(ColorDrawable(Color.BLACK), null)
            adaptive.setBounds(0, 0, size, size)
            val systemMask = adaptive.iconMask
            val maskBounds = RectF()
            systemMask.computeBounds(maskBounds, true)
            if (maskBounds.width() > 0f && maskBounds.height() > 0f) {
                val transform = Matrix().apply {
                    setRectToRect(
                        maskBounds,
                        RectF(0f, 0f, s, s),
                        Matrix.ScaleToFit.FILL,
                    )
                }
                systemMask.transform(transform, path)
            }
        }
    }
    return path
}

private fun rounded(path: Path, size: Float, radiusFraction: Float) {
    val radius = size * radiusFraction
    path.addRoundRect(RectF(0f, 0f, size, size), radius, radius, Path.Direction.CW)
}

private fun polygon(path: Path, size: Float, sides: Int) {
    val radius = min(size, size) * 0.5f
    for (i in 0 until sides) {
        val angle = Math.PI * 2.0 * i / sides - Math.PI / 2.0
        val x = size / 2f + cos(angle).toFloat() * radius
        val y = size / 2f + sin(angle).toFloat() * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
}
