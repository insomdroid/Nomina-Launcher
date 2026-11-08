package com.insomdroid.nomina.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
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
import com.insomdroid.nomina.model.IconRequest

/**
 * Coil fetcher that resolves an [IconRequest]'s launcher icon via [LauncherAppsSource]. The icon is
 * **rasterized to a Bitmap once** here rather than handed to Coil as a live Drawable: launcher icons
 * are usually AdaptiveIconDrawables that re-composite their layers on every draw, which makes a
 * scrolling grid stutter. A cached Bitmap is just a cheap blit per frame. The [IconRequest] carries
 * the themed/dark flags so the source can use the native monochrome layer or Nomina's renderer.
 */
class AppIconFetcher(
    private val data: IconRequest,
    private val source: LauncherAppsSource,
    private val custom: CustomIconSource,
) : Fetcher {
    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        // An icon the user picked for the app wins; if its pack or file is gone, the app's own shows.
        val drawable = data.app.customIcon?.let(custom::drawable)
            ?: source.loadIcon(data.app, data.themed, data.dark, data.iconPack, data.themeAll)
            ?: return@withContext null
        iconResult(drawable, data.autoResize)
    }

    class Factory(private val source: LauncherAppsSource, private val custom: CustomIconSource) : Fetcher.Factory<IconRequest> {
        override fun create(data: IconRequest, options: Options, imageLoader: ImageLoader): Fetcher =
            AppIconFetcher(data, source, custom)
    }
}

/** Rasterization size (px); crisp for icons up to ~56dp at xxxhdpi. */
private const val ICON_PX = 168

/** [drawable] drawn once into a bitmap (see [AppIconFetcher]) as Coil's result. */
internal fun iconResult(drawable: Drawable, autoResize: Boolean = false): FetchResult {
    val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, ICON_PX, ICON_PX)
    drawable.draw(canvas)
    val output = if (autoResize) normalizeIconBitmap(bitmap) else bitmap
    if (output !== bitmap) bitmap.recycle()
    return ImageFetchResult(
        image = output.asImage(),
        isSampled = false,
        dataSource = DataSource.DISK,
    )
}

/**
 * Normalizes transparent padding, common in legacy launcher icons. The visible alpha bounds are
 * scaled to the same 88% maximum visual size while preserving their aspect ratio and centring them.
 * Opaque artwork is deliberately not inspected or cropped: its original design remains intact.
 */
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
