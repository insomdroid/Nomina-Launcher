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
    fun acceptsTransparentArtworkWithBreathingRoom() {
        val pixels = IntArray(10 * 10)
        for (y in 2..7) {
            for (x in 2..7) {
                pixels[y * 10 + x] = 0xFF336699.toInt()
            }
        }

        assertThat(hasUsableTransparentGlyph(pixels, 10, 10)).isTrue()
    }

    @Test
    fun rejectsOpaqueArtworkInsteadOfGuessingItsBackground() {
        val pixels = IntArray(10 * 10) { 0xFF222222.toInt() }

        assertThat(hasUsableTransparentGlyph(pixels, 10, 10)).isFalse()
    }

    @Test
    fun rejectsAnEmptyTransparentCanvas() {
        val pixels = IntArray(10 * 10)

        assertThat(hasUsableTransparentGlyph(pixels, 10, 10)).isFalse()
    }

    @Test
    fun acceptsLargeTransparentGlyphSoItCanBeFittedIntoSafeZone() {
        val pixels = IntArray(10 * 10) { 0xFFFFFFFF.toInt() }
        for (index in 0 until 10) pixels[index] = 0

        assertThat(hasUsableTransparentGlyph(pixels, 10, 10)).isTrue()
    }

    @Test
    fun oversizedGlyphIsReducedProportionallyAndCentered() {
        val bounds = GlyphBounds(left = 0, top = 5, right = 99, bottom = 94)

        val destination = fitGlyphBounds(bounds, width = 100, height = 100)

        assertThat(destination.width).isWithin(0.01f).of(72f)
        assertThat(destination.height).isWithin(0.01f).of(64.8f)
        assertThat(destination.left).isWithin(0.01f).of(14f)
        assertThat(destination.top).isWithin(0.01f).of(17.6f)
    }

    @Test
    fun alreadySmallGlyphIsNotEnlarged() {
        val bounds = GlyphBounds(left = 20, top = 20, right = 79, bottom = 79)

        val destination = fitGlyphBounds(bounds, width = 100, height = 100)

        assertThat(destination.width).isWithin(0.01f).of(60f)
        assertThat(destination.height).isWithin(0.01f).of(60f)
        assertThat(destination.left).isWithin(0.01f).of(20f)
        assertThat(destination.top).isWithin(0.01f).of(20f)
    }

    @Test
    fun maskPreservesPartialAlphaWithoutIntroducingBackgroundPixels() {
        val pixels = IntArray(25)
        pixels[12] = 0x80FF0000.toInt()

        val mask = monochromeMaskPixels(pixels, 5, 5)

        assertThat(mask.count { (it ushr 24) > 0 }).isEqualTo(1)
        assertThat(mask[12]).isEqualTo(0x80FFFFFF.toInt())
    }
}
