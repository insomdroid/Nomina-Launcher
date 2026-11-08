package com.insomdroid.nomina.feature.settings

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.insomdroid.nomina.model.LauncherSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Geist is the only bundled font; custom fonts are imported separately from TTF files. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en")
class FontRowTest {
    @get:Rule val compose = createComposeRule()
    private val res = RuntimeEnvironment.getApplication().resources
    private val picked = mutableListOf<String>()

    private fun show(selected: String) {
        compose.setContent { FontRow(selected = selected, onPick = { picked += it }) }
        compose.waitForIdle()
    }

    @Test fun onlyGeistIsOfferedAsTheBundledFont() {
        show(LauncherSettings.FONT_DEFAULT)
        compose.onNodeWithText(res.getString(R.string.settings_font)).assertIsDisplayed()
        compose.onNodeWithTag(fontChipTag(LauncherSettings.FONT_DEFAULT)).assertIsSelected()
        compose.onNodeWithText(res.getString(R.string.settings_font_import)).assertIsDisplayed()
        assertThat(LauncherSettings.FONTS).containsExactly(
            LauncherSettings.FONT_DEFAULT,
            LauncherSettings.FONT_CUSTOM,
        ).inOrder()
    }

    @Test fun tappingGeistSelectsTheBundledFont() {
        show(LauncherSettings.FONT_CUSTOM)
        compose.onNodeWithTag(fontChipTag(LauncherSettings.FONT_DEFAULT)).performClick()
        assertThat(picked).containsExactly(LauncherSettings.FONT_DEFAULT)
    }

    @Test fun aLegacyStoredChoiceIsShownAsGeist() {
        show("serif")
        compose.onNodeWithTag(fontChipTag(LauncherSettings.FONT_DEFAULT)).performScrollTo().assertIsSelected()
    }
}
