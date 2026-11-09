package com.insomdroid.nomina.data

import android.os.Process
import coil3.request.Options
import com.google.common.truth.Truth.assertThat
import com.insomdroid.nomina.model.AppItem
import com.insomdroid.nomina.model.IconRequest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Every flag that changes what an icon looks like is in its cache key, so flipping a setting
 * re-renders the icon instead of serving the old bitmap (the icon-pack omission was once a bug).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppIconKeyerTest {
    private val app = AppItem("com.mail", "com.mail.Main", Process.myUserHandle(), 0L, "Mail")
    private val options = Options(RuntimeEnvironment.getApplication())
    private val keyer = AppIconKeyer()

    private fun key(request: IconRequest) = keyer.key(request, options)

    @Test fun reshapeModeAndMaskArePartOfTheKey() {
        val base = key(IconRequest(app))
        assertThat(key(IconRequest(app, reshapeLegacyIcons = "always"))).isNotEqualTo(base)
        assertThat(key(IconRequest(app, reshapeLegacyIcons = "off"))).isNotEqualTo(base)
        assertThat(key(IconRequest(app, iconShape = "circle"))).isNotEqualTo(base)
        assertThat(key(IconRequest(app, iconPack = "com.pack"))).isNotEqualTo(base)
        assertThat(key(IconRequest(app, epoch = 1))).isNotEqualTo(base)
    }

}
