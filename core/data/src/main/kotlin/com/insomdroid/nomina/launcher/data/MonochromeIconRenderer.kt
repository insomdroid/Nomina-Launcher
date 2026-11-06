package com.insomdroid.nomina.data

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.min

private const val ICON_RENDER_SIZE = 216
private const val MIN_TRANSPARENT_FRACTION = 0.08f
private const val MAX_TRANSPARENT_FRACTION = 0.92f
private const val MAX_GLYPH_SIZE_FRACTION = 0.72f

/**
 * Conservative fallback for apps without a native monochrome layer.
 *
 * Only genuinely transparent artwork is eligible. The visible alpha bounds are fitted into a
 * consistent safe area before tinting, preventing full-bleed legacy artwork from being enlarged
 * to the edge of the adaptive icon mask. Opaque artwork is rejected rather than guessed.
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
            val savedBounds = Rect(sourceDrawable.bounds)
            try {
                sourceDrawable.setBounds(0, 0, ICON_RENDER_SIZE, ICON_RENDER_SIZE)
                sourceDrawable.draw(Canvas(source))
            } finally {
                sourceDrawable.bounds = savedBounds
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

            if (!hasUsableTransparentGlyph(sourcePixels, ICON_RENDER_SIZE, ICON_RENDER_SIZE)) {
                return null
            }

            val bounds = alphaBounds(sourcePixels, ICON_RENDER_SIZE, ICON_RENDER_SIZE) ?: return null
            val destination = fitGlyphBounds(bounds, ICON_RENDER_SIZE, ICON_RENDER_SIZE)
            val output = Bitmap.createBitmap(
                ICON_RENDER_SIZE,
                ICON_RENDER_SIZE,
                Bitmap.Config.ARGB_8888,
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            Canvas(output).drawBitmap(
                source,
                Rect(bounds.left, bounds.top, bounds.right + 1, bounds.bottom + 1),
                RectF(destination.left, destination.top, destination.right, destination.bottom),
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

internal data class GlyphBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left + 1
    val height: Int get() = bottom - top + 1
}

internal data class GlyphDestination(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** Finds the visible alpha bounds without interpreting the artwork's RGB values. */
internal fun alphaBounds(
    pixels: IntArray,
    width: Int,
    height: Int,
): GlyphBounds? {
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

    return if (right < left || bottom < top) null else GlyphBounds(left, top, right, bottom)
}

/**
 * Fits a glyph into a centred safe area. Existing smaller artwork is never enlarged; oversized
 * artwork is reduced proportionally so its longest dimension occupies at most 72% of the canvas.
 */
internal fun fitGlyphBounds(
    bounds: GlyphBounds,
    width: Int,
    height: Int,
    maxSizeFraction: Float = MAX_GLYPH_SIZE_FRACTION,
): GlyphDestination {
    require(width > 0 && height > 0)
    require(maxSizeFraction > 0f && maxSizeFraction <= 1f)

    val maxSize = min(width, height) * maxSizeFraction
    val scale = min(1f, maxSize / max(bounds.width, bounds.height))
    val fittedWidth = bounds.width * scale
    val fittedHeight = bounds.height * scale
    val left = (width - fittedWidth) / 2f
    val top = (height - fittedHeight) / 2f
    return GlyphDestination(left, top, left + fittedWidth, top + fittedHeight)
}

/**
 * Accepts only artwork with a meaningful transparent canvas. A transparent hole in an otherwise
 * opaque background is not enough to identify a reliable glyph.
 */
internal fun hasUsableTransparentGlyph(
    pixels: IntArray,
    width: Int,
    height: Int,
): Boolean {
    require(width > 0 && height > 0 && pixels.size == width * height)

    var transparentCount = 0
    for (pixel in pixels) {
        if ((pixel ushr 24) < 250) transparentCount++
    }

    val transparentFraction = transparentCount.toFloat() / pixels.size
    if (transparentFraction !in MIN_TRANSPARENT_FRACTION..MAX_TRANSPARENT_FRACTION) {
        return false
    }
    return alphaBounds(pixels, width, height) != null
}

/** Converts only source alpha into a white mask; original RGB values are never interpreted. */
internal fun monochromeMaskPixels(
    pixels: IntArray,
    width: Int,
    height: Int,
): IntArray {
    require(width > 0 && height > 0 && pixels.size == width * height)

    return IntArray(pixels.size) { index ->
        val alpha = pixels[index] ushr 24
        if (alpha == 0) 0 else (alpha shl 24) or 0x00FFFFFF
    }
}
