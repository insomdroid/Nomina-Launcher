package com.insomdroid.nomina.model

/**
 * One match from a search source. Kept free of Android `Intent` plumbing so it stays comparable and
 * JVM-testable; the UI builds the actual Intent from [Setting.action].
 */
sealed interface SearchResult {
    val id: String

    data class App(val app: AppItem) : SearchResult {
        override val id: String get() = "app:${app.key}"
    }

    /** A system settings page. [action] is a `Settings.ACTION_*` (or equivalent) intent action. */
    data class Setting(val title: String, val action: String, val key: String) : SearchResult {
        override val id: String get() = "setting:$key"
    }

    data class Calculation(val expression: String, val result: String) : SearchResult {
        override val id: String get() = "calc"
    }
}

/** Search results grouped into the sections the drawer renders (top→bottom: calc, apps, settings). */
data class SearchResults(
    val calc: SearchResult.Calculation? = null,
    val apps: List<AppItem> = emptyList(),
    val settings: List<SearchResult.Setting> = emptyList(),
)
