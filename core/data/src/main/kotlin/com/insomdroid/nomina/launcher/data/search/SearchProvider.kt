package com.insomdroid.nomina.data.search

import com.insomdroid.nomina.model.SearchResult

/** A single search source (apps, settings, calculator). */
interface SearchProvider {
    /** Whether this source should run given the current settings/permission state. */
    suspend fun isEnabled(): Boolean

    /** Matches for [query] (already trimmed and non-blank). Must not throw for normal input. */
    suspend fun query(query: String): List<SearchResult>
}
