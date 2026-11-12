package com.insomdroid.nomina.ui.expressive

import androidx.compose.ui.unit.dp

/**
 * Shared layout tokens for launcher surfaces.
 *
 * Keep these values semantic: use the same token for the same layout role rather than
 * introducing another one-off dp value in each feature.
 */
object ExpressiveSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
}

/** Shared corner radii for controls, cards, and larger panels. */
object ExpressiveRadius {
    val control = 6.dp
    val card = 8.dp
    val panel = 16.dp
    val sheet = 24.dp
}
