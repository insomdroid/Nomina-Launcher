package com.insomdroid.nomina.data

import android.graphics.Bitmap
import android.graphics.Canvas
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
        iconResult(
            resolved.drawable,
            data.autoResize,
            data.iconShape.takeIf { reshape },
            resolved.iconBackColor,
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
    val output = reshape?.let {
        applyIconMask(
            normalized,
            it,
            iconBackColor ?: FALLBACK_ICON_BACKGROUND,
            artworkInset = if (isAdaptive) 0f else ICON_ARTWORK_INSET,
        )
    } ?: normalized
    if (output !== normalized) normalized.recycle()
    if (normalized !== bitmap) bitmap.recycle()
    return ImageFetchResult(image = output.asImage(), isSampled = false, dataSource = DataSource.DISK)
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
    val target = (ICON_PX * 0.88f).toInt()
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
