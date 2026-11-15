package com.insomdroid.nomina.baselineprofile

import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records Nomina's own startup and the launcher surfaces most often used after an update.
 * The generated profile is packaged in the release APK; this instrumentation module is not.
 */
@RunWith(AndroidJUnit4::class)
class NominaBaselineProfile {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Before
    fun dismissFirstRunOnboarding() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
            ?: error("$PACKAGE_NAME has no launch intent")

        context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        check(device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), WAIT_MS)) {
            "Nomina did not open before baseline profile recording"
        }

        val skip = By.text("Skip")
        if (device.wait(Until.hasObject(skip), 2_000L)) {
            device.findObject(skip)?.click()
            device.wait(Until.gone(skip), WAIT_MS)
        }
        device.pressHome()
        device.waitForIdle()
    }

    /** Cold launch path, recorded in the dedicated startup profile. */
    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
    ) {
        startActivityAndWait()
        device.waitForIdle()
    }

    /** App Drawer open/scroll/close and Settings screen initialization. */
    @Test
    fun homeDrawerAndSettings() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
    ) {
        startActivityAndWait()
        device.waitForIdle()

        val searchField = By.text("Search apps")
        val centerX = device.displayWidth / 2
        device.swipe(
            centerX,
            (device.displayHeight * 0.72f).toInt(),
            centerX,
            (device.displayHeight * 0.15f).toInt(),
            SWIPE_STEPS,
        )
        check(device.wait(Until.hasObject(searchField), WAIT_MS)) {
            "Nomina App Drawer did not open after swipe up"
        }
        device.waitForIdle()

        repeat(2) {
            device.swipe(
                centerX,
                (device.displayHeight * 0.82f).toInt(),
                centerX,
                (device.displayHeight * 0.30f).toInt(),
                SWIPE_STEPS,
            )
            device.waitForIdle()
        }

        device.pressBack()
        check(device.wait(Until.gone(searchField), WAIT_MS)) {
            "Nomina App Drawer did not close after Back"
        }
        device.waitForIdle()

        val settingsIntent = Intent().setClassName(
            PACKAGE_NAME,
            "$PACKAGE_NAME.SettingsActivity",
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        InstrumentationRegistry.getInstrumentation().targetContext.startActivity(settingsIntent)
        check(device.wait(Until.hasObject(By.textContains("SYSTEM")), WAIT_MS)) {
            "Nomina Settings did not open"
        }
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE_NAME = "com.insomdroid.nomina"
        const val WAIT_MS = 8_000L
        const val SWIPE_STEPS = 12
    }
}
