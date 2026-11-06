package com.insomdroid.nomina.model

/** Battery state used by the built-in battery widget. */
data class BatteryStatus(
    val percent: Int,
    val charging: Boolean,
)
