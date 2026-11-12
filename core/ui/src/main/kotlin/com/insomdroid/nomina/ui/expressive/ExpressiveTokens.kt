package com.insomdroid.nomina.ui.expressive

import androidx.compose.ui.unit.dp

/**
 * Shared spacing scale aligned with the Vibe Nothing layout rhythm.
 * Legacy semantic aliases remain available to avoid changing unrelated launcher surfaces.
 */
object ExpressiveSpacing {
    val none = 0.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s20 = 20.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val s40 = 40.dp
    val s64 = 64.dp
    val s80 = 80.dp
    val s96 = 96.dp
    val s128 = 128.dp

    val xxs = 4.dp
    val xs = s8
    val sm = s12
    val md = s16
    val lg = s20
    val xl = s24
}

/** Shared corner radii for controls and compact cards. */
object ExpressiveRadius {
    val control = 6.dp
    val card = 8.dp
    val panel = 8.dp
    val sheet = 8.dp
}
