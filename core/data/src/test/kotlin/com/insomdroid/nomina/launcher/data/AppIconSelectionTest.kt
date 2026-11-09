package com.insomdroid.nomina.data

import com.google.common.truth.Truth.assertThat
import com.insomdroid.nomina.model.LauncherSettings
import org.junit.Test

class AppIconSelectionTest {

    @Test
    fun alwaysReshapesLegacyIconsIncludingMappedPackIcons() {
        assertThat(shouldReshapeLegacyIcon(false, true, LauncherSettings.RESHAPE_ALWAYS)).isTrue()
        assertThat(shouldReshapeLegacyIcon(false, false, LauncherSettings.RESHAPE_ALWAYS)).isTrue()
    }

    @Test
    fun unthemedReshapesOnlyLegacyIconsNotMappedByTheIconPack() {
        assertThat(shouldReshapeLegacyIcon(false, false, LauncherSettings.RESHAPE_UNTHEMED)).isTrue()
        assertThat(shouldReshapeLegacyIcon(false, true, LauncherSettings.RESHAPE_UNTHEMED)).isFalse()
    }

    @Test
    fun adaptiveIconsKeepTheirOwnMaskInEveryMode() {
        LauncherSettings.RESHAPE_LEGACY_MODES.forEach { mode ->
            assertThat(shouldReshapeLegacyIcon(true, false, mode)).isFalse()
            assertThat(shouldReshapeLegacyIcon(true, true, mode)).isFalse()
        }
    }

    @Test
    fun offNeverReshapesLegacyIcons() {
        assertThat(shouldReshapeLegacyIcon(false, false, LauncherSettings.RESHAPE_OFF)).isFalse()
        assertThat(shouldReshapeLegacyIcon(false, true, LauncherSettings.RESHAPE_OFF)).isFalse()
    }

    @Test
    fun unknownModeFailsSafeToOff() {
        assertThat(shouldReshapeLegacyIcon(false, false, "unknown")).isFalse()
    }
}
