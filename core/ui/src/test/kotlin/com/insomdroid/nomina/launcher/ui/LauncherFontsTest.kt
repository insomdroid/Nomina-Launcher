package com.insomdroid.nomina.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import com.google.common.truth.Truth.assertThat
import com.insomdroid.nomina.designsystem.theme.withFontFamily
import org.junit.Test

/** Geist is the only bundled family; custom fonts require a validated imported file. */
class LauncherFontsTest {
    @Test fun onlyGeistIsBundledAndLegacyChoicesFallBackToGeist() {
        assertThat(LauncherSettings.FONTS).containsExactly(
            LauncherSettings.FONT_DEFAULT,
            LauncherSettings.FONT_CUSTOM,
        ).inOrder()
        assertThat(launcherFontFamily()).isNotEqualTo(FontFamily.Default)
        assertThat(launcherFontFamily(LauncherSettings.FONT_GEIST_MONO)).isEqualTo(launcherFontFamily())
        assertThat(launcherFontFamily(LauncherSettings.FONT_CUSTOM)).isEqualTo(launcherFontFamily())
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
