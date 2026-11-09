package com.insomdroid.nomina.data

import android.graphics.Bitmap
import android.graphics.Color
import com.insomdroid.nomina.model.LauncherSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

        assertThat(result.getPixel(50, 50)).isEqualTo(Color.RED)
        assertThat(result.getPixel(17, 17)).isEqualTo(Color.RED)
        assertThat(result.getPixel(50, 8)).isEqualTo(Color.BLACK)
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

        assertThat(result.getPixel(50, 5)).isEqualTo(Color.BLACK)
        assertThat(result.getPixel(17, 17)).isEqualTo(Color.RED)
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

        assertThat(result.getPixel(166, 84)).isEqualTo(Color.BLACK)
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

        assertThat(result.getPixel(50, 8)).isEqualTo(FALLBACK_ICON_BACKGROUND)
        source.recycle()
        result.recycle()
    }
}
