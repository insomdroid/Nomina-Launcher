package com.insomdroid.nomina.feature.home

import android.app.Application
import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import com.insomdroid.nomina.data.smartspace.CalendarRepository
import com.insomdroid.nomina.data.smartspace.WeatherRepository
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Date
import java.util.TimeZone

/**
 * The home and smartspace clocks follow the phone's current time format and time zone. The
 * smartspace re-formats immediately when the system time zone changes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en-rGB")
class ClockTest {
    @get:Rule val compose = createComposeRule()

    private val context: Application = RuntimeEnvironment.getApplication()
    private val defaultZone: TimeZone = TimeZone.getDefault()

    @After fun restoreZone() = TimeZone.setDefault(defaultZone)

    /** What the phone's own formatter says right now — zone and 12/24 h read afresh. */
    private fun phoneSays(at: Long): String = android.text.format.DateFormat.getTimeFormat(context).format(Date(at))

    @Test fun aTimeZoneChangeReformatsAtOnce() {
        val at = 1_800_000_000_000L
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Helsinki"))
        compose.setContent { Text(rememberTimeFormat().format(Date(at)), Modifier.testTag("time")) }
        val helsinki = phoneSays(at)
        compose.onNodeWithTag("time").assertTextEquals(helsinki)

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
        context.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
        compose.waitForIdle()

        val london = phoneSays(at)
        assertThat(london).isNotEqualTo(helsinki)
        compose.onNodeWithTag("time").assertTextEquals(london)
    }

    /** The big smartspace clock, which kept one formatter per UTC hour, follows the zone at once. */
    @Test fun theSmartspaceClockFollowsATimeZoneChange() {
        val testScope = TestScope()
        val wiring = ProductionWiring(context, testScope)
        val viewModel = SmartspaceViewModel(
            CalendarRepository(context, testScope.backgroundScope),
            WeatherRepository(context, testScope.backgroundScope),
            wiring.settingsRepository,
            wiring.appRepository,
        )
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Helsinki"))
        val at = System.currentTimeMillis()
        compose.setContent { SmartspaceWidget(viewModel = viewModel) }
        val helsinki = phoneSays(at)
        compose.onNodeWithText(helsinki).assertExists()

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
        context.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
        compose.waitForIdle()

        val london = phoneSays(at)
        assertThat(london).isNotEqualTo(helsinki)
        compose.onNodeWithText(london).assertExists()
        wiring.close()
    }
}
