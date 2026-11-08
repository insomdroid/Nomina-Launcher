package com.insomdroid.nomina.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import com.google.common.truth.Truth.assertThat
import com.insomdroid.nomina.designsystem.theme.withFontFamily
import org.junit.Test

/** The three built-in typefaces have fixed, separate roles. */
class LauncherFontsTest {
    @Test fun generalInterfaceUsesBundledGeist() {
        assertThat(launcherFontFamily()).isNotEqualTo(FontFamily.Default)
    }

    @Test fun smartspaceTechnicalAndDisplayFontsRemainDistinct() {
        val geist = launcherFontFamily()
        val mono = launcherTechnicalFontFamily()
        val display = launcherDisplayFontFamily()

        assertThat(mono).isNotEqualTo(geist)
        assertThat(display).isNotEqualTo(geist)
        assertThat(display).isNotEqualTo(mono)
    }

    @Test fun theTypographyTakesGeistInEveryMaterialStyle() {
        val geist = launcherFontFamily()
        val typography = Typography().withFontFamily(geist)
        val styles = listOf(
            typography.displayLarge, typography.displayMedium, typography.displaySmall,
            typography.headlineLarge, typography.headlineMedium, typography.headlineSmall,
            typography.titleLarge, typography.titleMedium, typography.titleSmall,
            typography.bodyLarge, typography.bodyMedium, typography.bodySmall,
            typography.labelLarge, typography.labelMedium, typography.labelSmall,
        )
        assertThat(styles.map { it.fontFamily }).containsExactlyElementsIn(List(15) { geist })
        assertThat(typography.bodyLarge.fontSize).isEqualTo(Typography().bodyLarge.fontSize)
        assertThat(typography.titleLarge.fontWeight).isEqualTo(Typography().titleLarge.fontWeight)
    }
}
