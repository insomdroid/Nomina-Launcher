package com.insomdroid.nomina.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MessageHeuristicsTest {

    @Test
    fun `call label matches the generic dialer strings in several languages`() {
        listOf("Missed call", "2 missed calls", "Vastaamaton puhelu", "Vastaamattomat puhelut", "Vastaamaton soitto",
            "Missat samtal", "Verpasster Anruf", "Appel manqué", "Llamada perdida").forEach {
            assertThat(MessageHeuristics.looksLikeCallLabel(it)).isTrue()
        }
    }

    @Test
    fun `call label does not match a caller whose name merely contains the word`() {
        listOf("Callum Reid", "Anna Callaghan", "Marcello", "Appelbaum", "Puhelinmyyjä").forEach {
            assertThat(MessageHeuristics.looksLikeCallLabel(it)).isFalse()
        }
    }

}
