package com.insomdroid.nomina.data

import com.insomdroid.nomina.data.local.AppLabelDao
import com.insomdroid.nomina.data.local.AppLabelEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Persistent storage for app labels, separated by the process locale. */
interface AppLabelStore {
    suspend fun read(locale: String): Map<String, String>
    suspend fun write(locale: String, labels: Map<String, String>)
}

class RoomAppLabelStore(private val dao: AppLabelDao) : AppLabelStore {
    override suspend fun read(locale: String): Map<String, String> =
        dao.getAll(locale).associate { it.key to it.label }

    override suspend fun write(locale: String, labels: Map<String, String>) {
        dao.replaceAll(
            labels.map { (key, label) -> AppLabelEntity(key = key, label = label, locale = locale) },
            locale,
        )
    }
}

/**
 * Keeps the last known activity labels between process starts. A package event invalidates that
 * package's entries and advances its generation, preventing an old LauncherActivityInfo snapshot
 * from repopulating the cache after an update.
 *
 * Keys use package/activity/profile-serial, so identically named activities in separate Android
 * profiles never share labels. All in-memory access is synchronized because package callbacks run
 * on the main thread while the serial app scan runs on Dispatchers.IO.
 */
@Singleton
class AppLabelCache @Inject constructor(private val store: AppLabelStore) {
    data class Reading(val label: String, val generation: Int)

    private val lock = Any()
    private var locale: String? = null
    private val labels = HashMap<String, String>()
    private val fromStore = HashSet<String>()
    private val generations = HashMap<String, Int>()
    private var persisted: Map<String, String> = emptyMap()

    suspend fun prepare(locale: String) {
        if (synchronized(lock) { this.locale == locale }) return
        val stored = runCatching { store.read(locale) }.getOrDefault(emptyMap())
        synchronized(lock) {
            this.locale = locale
            labels.clear()
            labels.putAll(stored)
            fromStore.clear()
            fromStore.addAll(stored.keys)
            persisted = stored
        }
    }

    /** Snapshot before getActivityList(): its LauncherActivityInfo values are package snapshots. */
    fun generations(): Map<String, Int> = synchronized(lock) { generations.toMap() }

    fun label(key: String): String? = synchronized(lock) { labels[key] }

    fun put(key: String, label: String, generation: Int) {
        synchronized(lock) {
            if (generation != (generations[packageOf(key)] ?: 0)) return
            labels[key] = label
            fromStore.remove(key)
        }
    }

    fun unverified(): Set<String> = synchronized(lock) { fromStore.toSet() }

    fun verify(fresh: Map<String, Reading>): Boolean = synchronized(lock) {
        var changed = false
        for ((key, reading) in fresh) {
            if (reading.generation != (generations[packageOf(key)] ?: 0)) continue
            if (labels[key] != reading.label) changed = true
            labels[key] = reading.label
            fromStore.remove(key)
        }
        changed
    }

    /** Drop entries for activities no longer listed, including profiles that have disappeared. */
    fun retain(keys: Set<String>) = synchronized(lock) {
        labels.keys.retainAll(keys)
        fromStore.retainAll(keys)
    }

    fun invalidatePackage(packageName: String) = synchronized(lock) {
        generations[packageName] = (generations[packageName] ?: 0) + 1
        val prefix = "$packageName/"
        labels.keys.removeAll { it.startsWith(prefix) }
        fromStore.removeAll { it.startsWith(prefix) }
    }

    suspend fun persist() {
        val snapshot = synchronized(lock) {
            val currentLocale = locale ?: return
            if (labels == persisted) return
            currentLocale to labels.toMap()
        }
        val written = runCatching { store.write(snapshot.first, snapshot.second) }.isSuccess
        if (written) synchronized(lock) { persisted = snapshot.second }
    }

    private fun packageOf(key: String): String = key.substringBefore('/')
}
