package com.insomdroid.nomina.data

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val ICON_RENDER_SIZE = 216
private const val SAFE_INSET_FRACTION = 0.14f
private const val BACKGROUND_DISTANCE_START = 0.045f
private const val BACKGROUND_DISTANCE_END = 0.20f

/** Bounds of the visible glyph in the renderer's source bitmap. Right and bottom are exclusive. */
internal data class IconPixelBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * Converts an icon into a clean monochrome glyph. Transparent artwork keeps its alpha; fully opaque
 * legacy artwork uses the median edge colour as a best-effort background estimate. The output is
 * cropped, aspect-ratio fitted into the adaptive safe zone, and tinted once before Coil caches it.
 */
internal object MonochromeIconRenderer {

    fun render(
        glyph: Drawable,
        tint: Int,
        resources: Resources,
    ): Drawable? {
        val source = Bitmap.createBitmap(
            ICON_RENDER_SIZE,
            ICON_RENDER_SIZE,
            Bitmap.Config.ARGB_8888,
        )
        try {
            val sourceDrawable = glyph.mutate()
            val oldBounds = Rect(sourceDrawable.bounds)
            try {
                sourceDrawable.setBounds(0, 0, ICON_RENDER_SIZE, ICON_RENDER_SIZE)
                sourceDrawable.draw(Canvas(source))
            } finally {
                sourceDrawable.bounds = oldBounds
            }

            val sourcePixels = IntArray(ICON_RENDER_SIZE * ICON_RENDER_SIZE)
            source.getPixels(
                sourcePixels,
                0,
                ICON_RENDER_SIZE,
                0,
                0,
                ICON_RENDER_SIZE,
                ICON_RENDER_SIZE,
            )
            val maskPixels = monochromeMaskPixels(
                sourcePixels,
                ICON_RENDER_SIZE,
                ICON_RENDER_SIZE,
            )
            val bounds = alphaBounds(maskPixels, ICON_RENDER_SIZE, ICON_RENDER_SIZE) ?: return null
            source.setPixels(
                maskPixels,
                0,
                ICON_RENDER_SIZE,
                0,
                0,
                ICON_RENDER_SIZE,
                ICON_RENDER_SIZE,
            )

            val destination = normalizedGlyphBounds(bounds, ICON_RENDER_SIZE)
            val output = Bitmap.createBitmap(
                ICON_RENDER_SIZE,
                ICON_RENDER_SIZE,
                Bitmap.Config.ARGB_8888,
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            Canvas(output).drawBitmap(
                source,
                Rect(bounds.left, bounds.top, bounds.right, bounds.bottom),
                RectF(
                    destination.left.toFloat(),
                    destination.top.toFloat(),
                    destination.right.toFloat(),
                    destination.bottom.toFloat(),
                ),
                paint,
            )

            val outputPixels = IntArray(ICON_RENDER_SIZE * ICON_RENDER_SIZE)
            output.getPixels(
                outputPixels,
                0,
                ICON_RENDER_SIZE,
                0,
                0,
                ICON_RENDER_SIZE,
                ICON_RENDER_SIZE,
            )
            val rgb = tint and 0x00FFFFFF
            for (index in outputPixels.indices) {
                val alpha = outputPixels[index] ushr 24
                outputPixels[index] = if (alpha == 0) 0 else (alpha shl 24) or rgb
            }
            output.setPixels(
                outputPixels,
                0,
                ICON_RENDER_SIZE,
                0,
                0,
                ICON_RENDER_SIZE,
                ICON_RENDER_SIZE,
            )
            return BitmapDrawable(resources, output)
        } finally {
            source.recycle()
        }
    }
}

/**
 * Makes a white alpha mask from ARGB pixels. If the source contains meaningful transparency, that
 * alpha is authoritative. Otherwise, a median sampled from the image border estimates a flat icon
 * background and a smooth contrast ramp separates the foreground from it.
 */
internal fun monochromeMaskPixels(
    pixels: IntArray,
    width: Int,
    height: Int,
): IntArray {
    require(width > 0 && height > 0 && pixels.size == width * height)

    val transparentPixels = pixels.count { (it ushr 24) < 250 }
    val hasTransparency = transparentPixels > pixels.size / 100
    val background = if (hasTransparency) null else medianBorderColor(pixels, width, height)

    return IntArray(pixels.size) { index ->
        val pixel = pixels[index]
        val sourceAlpha = pixel ushr 24
        val alpha = if (sourceAlpha == 0) {
            0
        } else if (hasTransparency) {
            sourceAlpha
        } else {
            val colorDistance = colorDistance(pixel, background!!)
            val progress = (
                (colorDistance - BACKGROUND_DISTANCE_START) /
                    (BACKGROUND_DISTANCE_END - BACKGROUND_DISTANCE_START)
                ).coerceIn(0f, 1f)
            val smooth = progress * progress * (3f - 2f * progress)
            (sourceAlpha * smooth).roundToInt().coerceIn(0, 255)
        }
        if (alpha == 0) 0 else (alpha shl 24) or 0x00FFFFFF
    }
}

/** Finds the smallest rectangle containing visible mask pixels. */
internal fun alphaBounds(
    pixels: IntArray,
    width: Int,
    height: Int,
): IconPixelBounds? {
    require(width > 0 && height > 0 && pixels.size == width * height)
    var left = width
    var top = height
    var right = -1
    var bottom = -1

    for (y in 0 until height) {
        for (x in 0 until width) {
            if ((pixels[y * width + x] ushr 24) > 8) {
                if (x < left) left = x
                if (y < top) top = y
                if (x > right) right = x
                if (y > bottom) bottom = y
            }
        }
    }
    if (right < left || bottom < top) return null
    return IconPixelBounds(left, top, right + 1, bottom + 1)
}

/** Fits the glyph into the same centred safe zone regardless of its original drawable bounds. */
internal fun normalizedGlyphBounds(
    source: IconPixelBounds,
    size: Int,
    safeInsetFraction: Float = SAFE_INSET_FRACTION,
): IconPixelBounds {
    require(size > 0 && source.width > 0 && source.height > 0)
    require(safeInsetFraction >= 0f && safeInsetFraction < 0.5f)

    val safeSize = size * (1f - 2f * safeInsetFraction)
    val scale = minOf(safeSize / source.width, safeSize / source.height)
    val fittedWidth = (source.width * scale).coerceAtMost(safeSize)
    val fittedHeight = (source.height * scale).coerceAtMost(safeSize)
    val left = ((size - fittedWidth) / 2f).roundToInt()
    val top = ((size - fittedHeight) / 2f).roundToInt()
    val right = (left + fittedWidth).roundToInt().coerceAtMost(size)
    val bottom = (top + fittedHeight).roundToInt().coerceAtMost(size)
    return IconPixelBounds(left, top, right, bottom)
}

private fun medianBorderColor(pixels: IntArray, width: Int, height: Int): Int {
    val samples = ArrayList<Int>(2 * width + 2 * height)
    for (x in 0 until width) {
        samples += pixels[x]
        if (height > 1) samples += pixels[(height - 1) * width + x]
    }
    for (y in 1 until height - 1) {
        samples += pixels[y * width]
        if (width > 1) samples += pixels[y * width + width - 1]
    }
    fun median(channel: (Int) -> Int): Int {
        val values = samples.map(channel).sorted()
        return values[values.size / 2]
    }
    return (0xFF shl 24) or
        (median { (it shr 16) and 0xFF } shl 16) or
        (median { (it shr 8) and 0xFF } shl 8) or
        median { it and 0xFF }
}

private fun colorDistance(first: Int, second: Int): Float {
    val red = ((first shr 16) and 0xFF) - ((second shr 16) and 0xFF)
    val green = ((first shr 8) and 0xFF) - ((second shr 8) and 0xFF)
    val blue = (first and 0xFF) - (second and 0xFF)
    return (sqrt((red * red + green * green + blue * blue).toFloat()) / 441.673f)
        .coerceIn(0f, 1f)
}
