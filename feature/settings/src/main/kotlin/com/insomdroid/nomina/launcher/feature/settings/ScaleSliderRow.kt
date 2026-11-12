package com.insomdroid.nomina.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.insomdroid.nomina.designsystem.theme.LauncherTextStyles
import com.insomdroid.nomina.ui.expressive.LocalExpressivePalette
import com.insomdroid.nomina.ui.expressive.ExpressiveRadius
import com.insomdroid.nomina.ui.expressive.ExpressiveSpacing
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.insomdroid.nomina.ui.launcherTechnicalFontFamily
import com.insomdroid.nomina.ui.launcherDisplayFontFamily
import kotlin.math.roundToInt

/** Slider tick count that lands the thumb on multiples of [step] across [range] (Material counts
 *  only the ticks strictly between the two ends). */
internal fun scaleSliderSteps(range: ClosedFloatingPointRange<Float>, step: Float): Int =
    (((range.endInclusive - range.start) / step).roundToInt() - 1).coerceAtLeast(0)

/**
 * The system's own long-press timeout (Android's accessibility "Touch & hold delay"), re-read every
 * time the screen resumes: it is not Compose state, and the user can change it in the system
 * settings while this screen waits in the background.
 */
@Composable
internal fun rememberSystemLongPressTimeout(): Long {
    val configuration = LocalViewConfiguration.current
    var timeout by remember(configuration) { mutableLongStateOf(configuration.longPressTimeoutMillis) }
    LifecycleResumeEffect(configuration) {
        timeout = configuration.longPressTimeoutMillis
        onPauseOrDispose { }
    }
    return timeout
}

/** A size multiplier as a whole percent (1.149 → 115). */
internal fun scalePercent(value: Float): Int = (value * 100f).roundToInt()

/**
 * A settings slider for a multiplier setting (icon size, label size, long-press delay): the current
 * value is shown beside the label in the words of [valueText], the thumb snaps to [step]
 * increments with a tick of haptic feedback on every change so a move is felt, and a "Default …"
 * chip under the slider appears whenever the value is off [default] to bring it straight back.
 */
@Composable
internal fun ScaleSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: (Float) -> String,
    description: String? = null,
    step: Float = 0.05f,
    default: Float = 1f,
) {
    val p = LocalExpressivePalette.current
    val haptics = LocalHapticFeedback.current
    val shown = valueText(value)
    // The readout last reported within the gesture in progress, null between gestures. A tick is felt
    // when the new readout differs from it — or, at the start of a gesture, from what is shown — so a
    // slider that repeats the same snapped value while the stored setting catches up ticks once, and a
    // value that arrived from outside (the stored setting loading, a restore) is judged as shown.
    var feltBase by remember { mutableStateOf<String?>(null) }
    val change: (Float) -> Unit = { next ->
        val text = valueText(next)
        if (text != (feltBase ?: shown)) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        feltBase = text
        onValueChange(next)
    }
    Surface(
        color = p.surfaceHi.copy(alpha = 0.94f),
        shape = RoundedCornerShape(ExpressiveRadius.card),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = ExpressiveSpacing.xs)
            
    ) {
        Column(modifier = Modifier.padding(horizontal = ExpressiveSpacing.xl, vertical = ExpressiveSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = p.text, style = LauncherTextStyles.settingsRowLabel, modifier = Modifier.weight(1f))
                Text(
                    shown,
                    color = p.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = launcherDisplayFontFamily(),
                    modifier = Modifier.padding(start = ExpressiveSpacing.sm),
                )
            }
            if (description != null) {
                Text(description, color = p.dim, style = LauncherTextStyles.settingsDescription, modifier = Modifier.padding(top = ExpressiveSpacing.xxs))
            }
            NothingSlider(
                value = value,
                onValueChange = change,
                valueRange = valueRange,
                steps = scaleSliderSteps(valueRange, step),
                onValueChangeFinished = { feltBase = null },
            )
            // Below the slider, so its appearance never moves the thumb away from the finger.
            if (shown != valueText(default)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                            change(default)
                            feltBase = null
                        }.padding(horizontal = ExpressiveSpacing.sm, vertical = ExpressiveSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.settings_reset_default, valueText(default)),
                            color = p.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = launcherTechnicalFontFamily(),
                        )
                    }
                }
            }
        }
    }
}
