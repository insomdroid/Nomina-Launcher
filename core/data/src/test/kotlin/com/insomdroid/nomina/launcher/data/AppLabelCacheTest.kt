package com.insomdroid.nomina.data

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AppLabelCacheTest {
    private class Store : AppLabelStore {
        val values = mutableMapOf<String, Map<String, String>>()
        var writes = 0
        override suspend fun read(locale: String): Map<String, String> = values[locale].orEmpty()
        override suspend fun write(locale: String, labels: Map<String, String>) {
            values[locale] = labels.toMap()
            writes++
        }
    }

    private val store = Store()
    private val cache = AppLabelCache(store)

    @Test fun persistedLabelsAreAvailableBeforeFreshVerification() = runTest {
        store.values["en-US"] = mapOf("com.mail/com.mail.Main/0" to "Mail")
        cache.prepare("en-US")
        assertThat(cache.label("com.mail/com.mail.Main/0")).isEqualTo("Mail")
        assertThat(cache.unverified()).containsExactly("com.mail/com.mail.Main/0")
    }

    @Test fun aPackageEventInvalidatesItsEntriesAndRejectsOldSnapshotReads() = runTest {
        cache.prepare("en-US")
        val snapshot = cache.generations()
        cache.invalidatePackage("com.mail")
        cache.put("com.mail/com.mail.Main/0", "Old Mail", snapshot["com.mail"] ?: 0)
        assertThat(cache.label("com.mail/com.mail.Main/0")).isNull()

        val current = cache.generations()
        cache.put("com.mail/com.mail.Main/0", "New Mail", current["com.mail"] ?: 0)
        assertThat(cache.label("com.mail/com.mail.Main/0")).isEqualTo("New Mail")
    }

    @Test fun verifyCorrectsPersistedLabelsAndRetainDropsMissingActivities() = runTest {
        store.values["en-US"] = mapOf(
            "com.mail/com.mail.Main/0" to "Old Mail",
            "com.gone/com.gone.Main/0" to "Gone",
        )
        cache.prepare("en-US")
        val changed = cache.verify(
            mapOf("com.mail/com.mail.Main/0" to AppLabelCache.Reading("Mail", 0)),
        )
        cache.retain(setOf("com.mail/com.mail.Main/0"))
        assertThat(changed).isTrue()
        assertThat(cache.label("com.mail/com.mail.Main/0")).isEqualTo("Mail")
        assertThat(cache.label("com.gone/com.gone.Main/0")).isNull()
    }

    @Test fun persistWritesOnlyWhenTheCacheChanges() = runTest {
        cache.prepare("en-US")
        cache.put("com.mail/com.mail.Main/0", "Mail", 0)
        cache.persist()
        cache.persist()
        assertThat(store.writes).isEqualTo(1)
        assertThat(store.values["en-US"]).containsExactly("com.mail/com.mail.Main/0", "Mail")
    }
}
