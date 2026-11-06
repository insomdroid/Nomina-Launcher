package com.insomdroid.nomina.data.smartspace

import java.util.Locale

/** A place name resolved by Android's local Geocoder. */
data class PlaceName(val name: String)

/**
 * Caches the platform Geocoder's last usable name for a rounded area. A transient geocoder failure
 * does not blank a name already shown, but a different area or an expired entry never reuses it.
 * The area key includes the UI language so a language switch triggers a fresh lookup.
 */
internal class PlaceNameCache(
    private val maxAgeMs: Long,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var name: String? = null
    private var areaKey: String? = null
    private var atMs = 0L

    fun resolve(areaKey: String, local: () -> String?): PlaceName? {
        local()?.let { return remember(it, areaKey) }
        val alive = now() - atMs < maxAgeMs
        val cached = name
        return if (cached != null && areaKey == this.areaKey && alive) PlaceName(cached) else null
    }

    private fun remember(name: String, areaKey: String): PlaceName {
        this.name = name
        this.areaKey = areaKey
        this.atMs = now()
        return PlaceName(name)
    }
}

/** The cache key for a rounded location in one UI language: a language switch (Android 13+ app
 *  language or the system language) must re-geocode instead of reusing "London" on a Finnish screen. */
internal fun placeAreaKey(roundedLat: Double, roundedLon: Double, languageTag: String): String =
    "%.2f,%.2f@%s".format(Locale.US, roundedLat, roundedLon, languageTag)
