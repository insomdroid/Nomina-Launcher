package com.insomdroid.nomina.data

/** Text heuristics the listener applies to notifications before they become people tiles. */
object MessageHeuristics {

    private val CALL_LABEL = Regex(
        """\b(calls?|puhelu[a-z]{0,2}|soitto[a-z]{0,2}|samtal|anrufe?|appels?|llamadas?)\b""",
        RegexOption.IGNORE_CASE,
    )
}
