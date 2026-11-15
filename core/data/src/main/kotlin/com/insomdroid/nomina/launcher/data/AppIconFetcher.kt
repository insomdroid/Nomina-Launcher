package com.insomdroid.nomina.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import com.insomdroid.nomina.model.CustomIcon
import com.insomdroid.nomina.model.IconRequest

class AppIconFetcher(
    private val data: IconRequest,
    private val source: LauncherAppsSource,
    private val custom: CustomIconSource,
) : Fetcher {
    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val customDrawable = data.app.customIcon?.let(custom::drawable)
        val resolved = if (customDrawable != null) {
            ResolvedAppIcon(
                customDrawable,
                data.app.customIcon is CustomIcon.FromPack,
                customDrawable is AdaptiveIconDrawable,
                source.iconBackColor(data.iconPack),
            )
        } else {
            source.loadIcon(data.app, data.iconPack) ?: return@withContext null
        }
        val reshape = shouldReshapeLegacyIcon(
            isAdaptive = resolved.isAdaptive,
            isPackMapped = resolved.isPackMapped,
            mode = data.reshapeLegacyIcons,
        )
        val packIsMonochrome = !resolved.isPackMapped && source.isMonochromeIconPack(data.iconPack)
        val monochrome = !resolved.isPackMapped && when (data.unthemedIconMode) {
            com.insomdroid.nomina.model.LauncherSettings.UNTHEMED_ICON_MONOCHROME -> true
            com.insomdroid.nomina.model.LauncherSettings.UNTHEMED_ICON_ORIGINAL -> false
            else -> packIsMonochrome
        }
        iconResult(
            resolved.drawable,
            data.autoResize,
            data.iconShape.takeIf { reshape },
            resolved.iconBackColor,
            monochrome = monochrome,
            containerMode = data.unthemedContainerMode,
            containerColor = data.unthemedContainerColor,
            containerOpacity = data.unthemedContainerOpacity,
            packIsMonochrome = packIsMonochrome,
            applyUnthemedContainer = !resolved.isPackMapped,
        )
    }

    class Factory(private val source: LauncherAppsSource, private val custom: CustomIconSource) : Fetcher.Factory<IconRequest> {
        override fun create(data: IconRequest, options: Options, imageLoader: ImageLoader): Fetcher =
            AppIconFetcher(data, source, custom)
    }
}

private const val ICON_PX = 168

internal fun iconResult(
    drawable: Drawable,
    autoResize: Boolean = false,
    reshape: String? = null,
    iconBackColor: Int? = null,
    monochrome: Boolean = reshape != null,
    containerMode: String = com.insomdroid.nomina.model.LauncherSettings.UNTHEMED_CONTAINER_AUTOMATIC,
    containerColor: Int = 0xFF808080.toInt(),
    containerOpacity: Float = com.insomdroid.nomina.model.LauncherSettings.DEFAULT_UNTHEMED_CONTAINER_OPACITY,
    packIsMonochrome: Boolean = false,
    applyUnthemedContainer: Boolean = true,
): FetchResult {
    val isAdaptive = drawable is AdaptiveIconDrawable
    val artwork = if (isAdaptive && reshape != null) {
        drawable.foreground ?: drawable
    } else {
        drawable
    }
    val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
    artwork.setBounds(0, 0, ICON_PX, ICON_PX)
    artwork.draw(Canvas(bitmap))
    // Adaptive foreground assets already contain the platform's safe-zone padding. Normalizing
    // or applying the legacy inset again would shrink their artwork twice.
    val normalized = if (autoResize && !isAdaptive) normalizeIconBitmap(bitmap) else bitmap
    val artworkBitmap = if (monochrome) toMonochrome(normalized) else normalized
    val automaticBackplate = if (containerMode == com.insomdroid.nomina.model.LauncherSettings.UNTHEMED_CONTAINER_AUTOMATIC &&
        applyUnthemedContainer && !packIsMonochrome) {
        dominantIconColor(normalized) ?: iconBackColor ?: FALLBACK_ICON_BACKGROUND
    } else {
        iconBackColor ?: FALLBACK_ICON_BACKGROUND
    }
    val selectedBackplate = if (!applyUnthemedContainer) {
        iconBackColor ?: FALLBACK_ICON_BACKGROUND
    } else when (containerMode) {
        com.insomdroid.nomina.model.LauncherSettings.UNTHEMED_CONTAINER_NONE -> android.graphics.Color.TRANSPARENT
        com.insomdroid.nomina.model.LauncherSettings.UNTHEMED_CONTAINER_CUSTOM -> containerColor
        else -> if (packIsMonochrome) FALLBACK_ICON_BACKGROUND else automaticBackplate
    }
    val backplateWithOpacity = if (applyUnthemedContainer) {
        val alpha = (android.graphics.Color.alpha(selectedBackplate) * containerOpacity.coerceIn(0.2f, 1f)).roundToInt()
        (selectedBackplate and 0x00FFFFFF) or (alpha shl 24)
    } else {
        selectedBackplate
    }
    val output = reshape?.let {
        applyIconMask(
            artworkBitmap,
            it,
            backplateWithOpacity,
            artworkInset = if (isAdaptive) 0f else ICON_ARTWORK_INSET,
        )
    } ?: artworkBitmap
    if (output !== artworkBitmap) artworkBitmap.recycle()
    if (artworkBitmap !== normalized) normalized.recycle()
    if (normalized !== bitmap) bitmap.recycle()
    return ImageFetchResult(image = output.asImage(), isSampled = false, dataSource = DataSource.DISK)
}


/** Samples the most common central colour for a subtle app-specific backplate. */
private fun dominantIconColor(source: Bitmap): Int? {
    val buckets = HashMap<Int, IconColorBucket>()
    val left = source.width / 4
    val right = source.width * 3 / 4
    val top = source.height / 4
    val bottom = source.height * 3 / 4
    for (y in top until bottom) for (x in left until right) {
        val pixel = source.getPixel(x, y)
        if (android.graphics.Color.alpha(pixel) < 180) continue
        val red = android.graphics.Color.red(pixel)
        val green = android.graphics.Color.green(pixel)
        val blue = android.graphics.Color.blue(pixel)
        val max = maxOf(red, green, blue)
        val min = minOf(red, green, blue)
        if (max < 28 || (max - min).toFloat() / max < 0.12f) continue
        val key = ((red shr 4) shl 8) or ((green shr 4) shl 4) or (blue shr 4)
        val bucket = buckets.getOrPut(key) { IconColorBucket() }
        bucket.red += red.toLong()
        bucket.green += green.toLong()
        bucket.blue += blue.toLong()
        bucket.count++
    }
    val dominant = buckets.values.maxByOrNull { it.count } ?: return null
    if (dominant.count == 0L) return null
    return android.graphics.Color.rgb(
        (dominant.red / dominant.count).toInt(),
        (dominant.green / dominant.count).toInt(),
        (dominant.blue / dominant.count).toInt(),
    )
}

private class IconColorBucket {
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0L
}

/** Removes hue from unthemed artwork while preserving its luminance, edges and transparency. */
private fun toMonochrome(source: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }
    Canvas(output).drawBitmap(source, 0f, 0f, paint)
    return output
}

/** Normalizes transparent padding while preserving aspect ratio and centring the visible artwork. */
private fun normalizeIconBitmap(source: Bitmap): Bitmap {
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    var left = source.width
    var top = source.height
    var right = -1
    var bottom = -1
    for (y in 0 until source.height) {
        val row = y * source.width
        for (x in 0 until source.width) {
            if ((pixels[row + x] ushr 24) > 12) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }
    if (right < left || bottom < top) return source
    val bounds = Rect(left, top, right + 1, bottom + 1)
    // Android adaptive icons use a 66dp keyline inside a 108dp canvas.
    // Match legacy visible artwork to that same safe-zone scale instead of filling 88% of the tile.
    val target = (ICON_PX * (66f / 108f)).roundToInt()
    val scale = minOf(target.toFloat() / bounds.width(), target.toFloat() / bounds.height())
    val width = (bounds.width() * scale).toInt().coerceAtLeast(1)
    val height = (bounds.height() * scale).toInt().coerceAtLeast(1)
    val destination = Rect(
        (ICON_PX - width) / 2,
        (ICON_PX - height) / 2,
        (ICON_PX + width) / 2,
        (ICON_PX + height) / 2,
    )
    val normalized = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
    Canvas(normalized).drawBitmap(
        source,
        bounds,
        destination,
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
    )
    return normalized
}
