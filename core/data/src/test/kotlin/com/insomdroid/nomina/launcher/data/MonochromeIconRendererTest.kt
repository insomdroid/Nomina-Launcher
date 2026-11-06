package com.insomdroid.nomina.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MonochromeIconRendererTest {

    @Test
    fun transparentArtworkKeepsItsAlphaAndDropsItsOriginalColour() {
        val pixels = intArrayOf(
            0xFFFF0000.toInt(),
            0x800000FF.toInt(),
            0x00123456,
        )

        val mask = monochromeMaskPixels(pixels, width = 3, height = 1)

        assertThat(mask[0]).isEqualTo(0xFFFFFFFF.toInt())
        assertThat(mask[1]).isEqualTo(0x80FFFFFF.toInt())
        assertThat(mask[2]).isEqualTo(0)
    }

    @Test
    fun opaqueArtworkSeparatesAContrastingGlyphFromAFlatEdgeBackground() {
        val background = 0xFF202020.toInt()
        val glyph = 0xFFFFFFFF.toInt()
        val pixels = IntArray(25) { background }
        pixels[12] = glyph

        val mask = monochromeMaskPixels(pixels, width = 5, height = 5)

        assertThat(mask[0]).isEqualTo(0)
        assertThat(mask[12]).isEqualTo(0xFFFFFFFF.toInt())
        assertThat(alphaBounds(mask, 5, 5)).isEqualTo(IconPixelBounds(2, 2, 3, 3))
    }

    @Test
    fun aUniformOpaqueIconDoesNotProduceAFalseGlyph() {
        val pixels = IntArray(64) { 0xFF777777.toInt() }

        val mask = monochromeMaskPixels(pixels, width = 8, height = 8)

        assertThat(alphaBounds(mask, 8, 8)).isNull()
    }

    @Test
    fun alphaBoundsUsesExclusiveRightAndBottomEdges() {
        val pixels = IntArray(25)
        pixels[1 * 5 + 2] = 0xFFFFFFFF.toInt()
        pixels[3 * 5 + 4] = 0xFFFFFFFF.toInt()

        assertThat(alphaBounds(pixels, 5, 5))
            .isEqualTo(IconPixelBounds(left = 2, top = 1, right = 5, bottom = 4))
    }

    @Test
    fun normalizedGlyphBoundsCentersTheGlyphAndKeepsItInsideTheSafeZone() {
        val result = normalizedGlyphBounds(
            source = IconPixelBounds(left = 20, top = 10, right = 80, bottom = 40),
            size = 100,
            safeInsetFraction = 0.1f,
        )

        assertThat(result.left).isEqualTo(10)
        assertThat(result.top).isEqualTo(30)
        assertThat(result.right).isEqualTo(90)
        assertThat(result.bottom).isEqualTo(70)
    }
}
