package com.insomdroid.nomina.data

import android.graphics.Bitmap
import android.graphics.Color
import com.insomdroid.nomina.model.LauncherSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class IconMaskRendererTest {

    @Test
    fun legacyArtworkKeepsItsCornersInsidePackColouredCircle() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }

        val result = applyIconMask(
            source = source,
            shape = LauncherSettings.ICON_SHAPE_CIRCLE,
            backgroundColor = Color.BLACK,
        )

        val center = result.getPixel(50, 50)
        val insetArtwork = result.getPixel(25, 25)
        val backplate = result.getPixel(50, 8)
        assertThat(Color.alpha(center)).isEqualTo(255)
        assertThat(Color.red(center)).isGreaterThan(Color.green(center))
        assertThat(Color.red(insetArtwork)).isGreaterThan(0)
        assertThat(Color.alpha(backplate)).isEqualTo(255)
        assertThat(Color.red(backplate)).isEqualTo(0)
        assertThat(Color.alpha(result.getPixel(0, 0))).isEqualTo(0)
        source.recycle()
        result.recycle()
    }

    @Test
    fun selectedShapeControlsBackplateWithoutClippingArtwork() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }

        val result = applyIconMask(
            source = source,
            shape = LauncherSettings.ICON_SHAPE_ROUNDED_SQUARE,
            backgroundColor = Color.BLACK,
        )

        val topBackplate = result.getPixel(50, 5)
        val artwork = result.getPixel(25, 25)
        assertThat(Color.alpha(topBackplate)).isEqualTo(255)
        assertThat(Color.red(topBackplate)).isEqualTo(0)
        assertThat(Color.alpha(artwork)).isEqualTo(255)
        assertThat(Color.red(artwork)).isGreaterThan(Color.green(artwork))
        assertThat(Color.alpha(result.getPixel(0, 0))).isEqualTo(0)
        source.recycle()
        result.recycle()
    }

    @Test
    fun systemShapeMaskIsScaledToTheFullIconBitmap() {
        val source = Bitmap.createBitmap(168, 168, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }

        val result = applyIconMask(
            source = source,
            shape = LauncherSettings.ICON_SHAPE_SYSTEM,
            backgroundColor = Color.BLACK,
        )

        val center = result.getPixel(84, 84)
        assertThat(Color.red(center)).isGreaterThan(Color.green(center))
        assertThat(Color.alpha(result.getPixel(166, 84))).isEqualTo(255)
        assertThat(Color.alpha(result.getPixel(167, 0))).isEqualTo(0)
        source.recycle()
        result.recycle()
    }

    @Test
    fun missingPackBackplateUsesNeutralFallback() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }

        val result = applyIconMask(source, LauncherSettings.ICON_SHAPE_CIRCLE)

        val fallback = result.getPixel(50, 8)
        assertThat(Color.alpha(fallback)).isEqualTo(255)
        assertThat(Color.red(fallback)).isEqualTo(Color.red(FALLBACK_ICON_BACKGROUND))
        assertThat(Color.green(fallback)).isEqualTo(Color.green(FALLBACK_ICON_BACKGROUND))
        assertThat(Color.blue(fallback)).isEqualTo(Color.blue(FALLBACK_ICON_BACKGROUND))
        source.recycle()
        result.recycle()
    }
}
