package com.insomdroid.nomina.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppIconSelectionTest {

    @Test
    fun mappedIconPackEntryTakesPriority() {
        assertThat(selectAppIcon("pack", "native", "generated", "original"))
            .isEqualTo("pack")
    }

    @Test
    fun nativeMonochromeTakesPriorityWhenPackHasNoMapping() {
        assertThat(selectAppIcon(null, "native", "generated", "original"))
            .isEqualTo("native")
    }

    @Test
    fun generatedSilhouetteIsUsedWhenNativeMonochromeIsMissing() {
        assertThat(selectAppIcon(null, null, "generated", "original"))
            .isEqualTo("generated")
    }

    @Test
    fun originalIconIsTheFinalFallback() {
        assertThat(selectAppIcon(null, null, null, "original"))
            .isEqualTo("original")
    }

    @Test
    fun missingEveryIconSourceReturnsNull() {
        assertThat(selectAppIcon<String>(null, null, null, null))
            .isNull()
    }
}
