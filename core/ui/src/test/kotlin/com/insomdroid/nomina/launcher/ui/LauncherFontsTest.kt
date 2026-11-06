package com.insomdroid.nomina.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import com.google.common.truth.Truth.assertThat
import com.insomdroid.nomina.designsystem.theme.withFontFamily
import com.insomdroid.nomina.model.LauncherSettings
import org.junit.Test

/** The selectable launcher typography is limited to Geist and Geist Mono; removed legacy keys safely use Geist. */
class LauncherFontsTest {
    @Test fun onlyGeistFamiliesRemainSelectableAndLegacyKeysFallBackToGeist() {
        assertThat(LauncherSettings.FONTS).containsExactly(
            LauncherSettings.FONT_DEFAULT,
            LauncherSettings.FONT_GEIST_MONO,
        ).inOrder()
        assertThat(launcherFontFamily(LauncherSettings.FONT_DEFAULT)).isNotEqualTo(FontFamily.Default)
        assertThat(launcherFontFamily(LauncherSettings.FONT_GEIST_MONO)).isNotEqualTo(FontFamily.Default)
        listOf("condensed", "serif", "monospace", "casual", "cursive", "unknown").forEach { legacy ->
            assertThat(launcherFontFamily(legacy)).isEqualTo(launcherFontFamily(LauncherSettings.FONT_DEFAULT))
        }
    }

    @Test fun theTypographyTakesTheFamilyInEveryStyle() {
        val typography = Typography().withFontFamily(FontFamily.Serif)
        val styles = listOf(
            typography.displayLarge, typography.displayMedium, typography.displaySmall,
            typography.headlineLarge, typography.headlineMedium, typography.headlineSmall,
            typography.titleLarge, typography.titleMedium, typography.titleSmall,
            typography.bodyLarge, typography.bodyMedium, typography.bodySmall,
            typography.labelLarge, typography.labelMedium, typography.labelSmall,
        )
        assertThat(styles.map { it.fontFamily }).containsExactlyElementsIn(List(15) { FontFamily.Serif })
        assertThat(typography.bodyLarge.fontSize).isEqualTo(Typography().bodyLarge.fontSize)
        assertThat(typography.titleLarge.fontWeight).isEqualTo(Typography().titleLarge.fontWeight)
    }
}
