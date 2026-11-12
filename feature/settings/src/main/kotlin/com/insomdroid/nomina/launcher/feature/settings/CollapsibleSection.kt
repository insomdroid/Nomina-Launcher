package com.insomdroid.nomina.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders a category's settings directly. Category navigation already provides
 * the page boundary, so a second collapsible layer only hides the actual controls.
 *
 * The legacy parameters remain in the signature to avoid changing every settings
 * call site or persisted preference schema.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun CollapsibleSection(
    title: String,
    @DrawableRes icon: Int,
    collapsed: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier, content = content)
}
