package com.insomdroid.nomina.data

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

private const val ICON_RENDER_SIZE = 216
private const val MIN_TRANSPARENT_FRACTION = 0.08f
private const val MAX_TRANSPARENT_FRACTION = 0.92f
private const val MAX_GLYPH_BOUNDS_FRACTION = 0.96f

/**
 * Conservative fallback for apps without a native monochrome layer.
 *
 * The renderer only accepts artwork with a genuinely transparent canvas and a glyph that leaves
 * visible outer breathing room. It never guesses a background colour from edge pixels: that
 * heuristic turned flat backgrounds and dark artwork into solid silhouettes. The original canvas
 * is preserved without cropping or scaling, so adaptive foreground safe zones remain intact.
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
            val oldBounds = sourceDrawable.bounds
            val savedBounds = android.graphics.Rect(oldBounds)
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

            val outputPixels = monochromeMaskPixels(
                sourcePixels,
                ICON_RENDER_SIZE,
                ICON_RENDER_SIZE,
            )
            val output = Bitmap.createBitmap(
                ICON_RENDER_SIZE,
                ICON_RENDER_SIZE,
                Bitmap.Config.ARGB_8888,
            )
            output.setPixels(
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
 * Returns true only for a transparent glyph with a real outer margin. A transparent hole inside an
 * otherwise full-bleed icon is not sufficient: accepting it would preserve its background as part
 * of the monochrome shape.
 */
internal fun hasUsableTransparentGlyph(
    pixels: IntArray,
    width: Int,
    height: Int,
): Boolean {
    require(width > 0 && height > 0 && pixels.size == width * height)

    var transparentCount = 0
    var left = width
    var top = height
    var right = -1
    var bottom = -1

    for (y in 0 until height) {
        for (x in 0 until width) {
            val alpha = pixels[y * width + x] ushr 24
            if (alpha < 250) {
                transparentCount++
            }
            if (alpha > 8) {
                if (x < left) left = x
                if (y < top) top = y
                if (x > right) right = x
                if (y > bottom) bottom = y
            }
        }
    }

    val transparentFraction = transparentCount.toFloat() / pixels.size
    if (transparentFraction !in MIN_TRANSPARENT_FRACTION..MAX_TRANSPARENT_FRACTION) {
        return false
    }
    if (right < left || bottom < top) return false

    val boundsWidth = right - left + 1
    val boundsHeight = bottom - top + 1
    return boundsWidth.toFloat() / width <= MAX_GLYPH_BOUNDS_FRACTION &&
        boundsHeight.toFloat() / height <= MAX_GLYPH_BOUNDS_FRACTION
}

/** Converts only the source alpha into a white mask; original RGB values are never interpreted. */
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
