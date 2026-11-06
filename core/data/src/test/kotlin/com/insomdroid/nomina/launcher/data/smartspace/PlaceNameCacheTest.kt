package com.insomdroid.nomina.data.smartspace

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaceNameCacheTest {
    private var now = 0L
    private val cache = PlaceNameCache(maxAgeMs = 1_000) { now }

    @Test fun aLocalNameIsCached() {
        assertThat(cache.resolve("60.21,24.87", local = { "Vantaa" }))
            .isEqualTo(PlaceName("Vantaa"))
    }

    @Test fun aLocalNameSurvivesATransientFailureInTheSameArea() {
        cache.resolve("60.21,24.87", local = { "Vantaa" })

        assertThat(cache.resolve("60.21,24.87", local = { null }, network = null)?.name).isEqualTo("Vantaa")
    }

    @Test fun anotherAreaDoesNotKeepThePreviousName() {
        cache.resolve("60.21,24.87", local = { "Vantaa" }, network = null)

        assertThat(cache.resolve("61.50,23.76", local = { null }, network = null)).isNull()
    }

    @Test fun aNameResolvedInAnotherLanguageIsNotReused() {
        cache.resolve(placeAreaKey(60.21, 24.87, "en-GB"), local = { "Helsinki" }, network = null)

        assertThat(cache.resolve(placeAreaKey(60.21, 24.87, "fi-FI"), local = { null }, network = null)).isNull()
        assertThat(cache.resolve(placeAreaKey(60.21, 24.87, "en-GB"), local = { null }, network = null)?.name)
            .isEqualTo("Helsinki")
    }

    @Test fun theAreaKeyRoundsTheSameWayInEveryLanguage() {
        assertThat(placeAreaKey(60.214, 24.869, "fi-FI")).isEqualTo("60.21,24.87@fi-FI")
        assertThat(placeAreaKey(60.214, 24.869, "tr-TR")).isEqualTo("60.21,24.87@tr-TR")
    }

    @Test fun anOldNameExpires() {
        cache.resolve("60.21,24.87", local = { "Vantaa" }, network = null)
        now = 1_000

        assertThat(cache.resolve("60.21,24.87", local = { null }, network = null)).isNull()
    }
}
