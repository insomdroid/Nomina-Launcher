package com.insomdroid.nomina.feature.settings

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The available Nomina feedback channel opens the project's own GitHub Discussions page. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en")
class FeedbackRowsTest {
    @get:Rule val compose = createComposeRule()

    private val res = RuntimeEnvironment.getApplication().resources
    private val opened = mutableListOf<String>()

    private fun show() {
        compose.setContent { Column { FeedbackRows(onOpen = { opened += it }) } }
        compose.waitForIdle()
    }

    @Test fun onlyTheNominaDiscussionChannelIsListed() {
        show()

        compose.onNodeWithText(res.getString(R.string.settings_feedback_discussions)).assertIsDisplayed()
        compose.onNodeWithText(res.getString(R.string.settings_feedback_discussions_desc)).assertIsDisplayed()
        compose.onNodeWithText("Telegram group").assertDoesNotExist()
        compose.onNodeWithText("Reddit community").assertDoesNotExist()
    }

    @Test fun discussionRowOpensTheNominaRepository() {
        show()

        compose.onNodeWithText(res.getString(R.string.settings_feedback_discussions)).performClick()

        assertThat(opened).containsExactly(DISCUSSIONS_URL)
        assertThat(DISCUSSIONS_URL).isEqualTo("https://github.com/insomdroid/Nomina-Launcher/discussions")
    }
}
