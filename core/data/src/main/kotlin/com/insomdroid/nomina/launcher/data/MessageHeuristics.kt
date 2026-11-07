package com.insomdroid.nomina.data

/** Text heuristics the listener applies to notifications before they become people tiles. */
object MessageHeuristics {

    /**
     * True for the generic missed-call label a dialer puts in whichever field isn't the caller.
     * Whole words only, so a caller named Callum or Appelbaum is not mistaken for the label.
     */
    fun looksLikeCallLabel(s: String): Boolean = CALL_LABEL.containsMatchIn(s)

    private val CALL_LABEL = Regex(
        """\b(calls?|puhelu[a-z]{0,2}|soitto[a-z]{0,2}|samtal|anrufe?|appels?|llamadas?)\b""",
        RegexOption.IGNORE_CASE,
    )
}
