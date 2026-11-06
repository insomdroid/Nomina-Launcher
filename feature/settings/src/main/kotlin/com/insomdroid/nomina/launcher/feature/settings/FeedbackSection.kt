package com.insomdroid.nomina.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.insomdroid.nomina.ui.expressive.ExpressiveActionRow

internal const val DISCUSSIONS_URL = "https://github.com/insomdroid/Nomina-Launcher/discussions"

/** The single feedback channel currently available for Nomina. */
@Composable
internal fun FeedbackRows(onOpen: (String) -> Unit) {
    ExpressiveActionRow(
        label = stringResource(R.string.settings_feedback_discussions),
        description = stringResource(R.string.settings_feedback_discussions_desc),
    ) { onOpen(DISCUSSIONS_URL) }
}
