package com.insomdroid.nomina.ui.expressive

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Variants shared by launcher actions. */
enum class ExpressiveButtonStyle {
    Primary,
    Secondary,
    Ghost,
}

/** Flat, compact action button. No elevation or decorative accent colour. */
@Composable
fun ExpressiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ExpressiveButtonStyle = ExpressiveButtonStyle.Primary,
) {
    val p = LocalExpressivePalette.current
    val background = when (style) {
        ExpressiveButtonStyle.Primary -> p.text
        ExpressiveButtonStyle.Secondary -> p.btn
        ExpressiveButtonStyle.Ghost -> p.bg
    }
    val foreground = when (style) {
        ExpressiveButtonStyle.Primary -> p.bg
        ExpressiveButtonStyle.Secondary, ExpressiveButtonStyle.Ghost -> p.text
    }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(RoundedCornerShape(ExpressiveRadius.control))
            .background(background.copy(alpha = if (enabled) 1f else 0.42f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = foreground.copy(alpha = if (enabled) 1f else 0.55f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Text-only action for secondary and destructive-dialog actions. */
@Composable
fun ExpressiveTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val p = LocalExpressivePalette.current
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(RoundedCornerShape(ExpressiveRadius.control))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = p.text.copy(alpha = if (enabled) 1f else 0.4f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Monochrome switch with a 200 ms ease-in-out thumb movement. */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactive: Boolean = true,
    hitTargetWidth: androidx.compose.ui.unit.Dp = 42.dp,
    hitTargetHeight: androidx.compose.ui.unit.Dp = 24.dp,
    trackWidth: androidx.compose.ui.unit.Dp = 42.dp,
    trackHeight: androidx.compose.ui.unit.Dp = 24.dp,
    thumbSize: androidx.compose.ui.unit.Dp = 20.dp,
) {
    val p = LocalExpressivePalette.current
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - 2.dp else 2.dp,
        animationSpec = tween(ExpressiveMotion.microDurationMillis, easing = ExpressiveMotion.easeInOut),
        label = "expressiveSwitchThumb",
    )
    Box(
        modifier = modifier
            .size(width = hitTargetWidth, height = hitTargetHeight)
            .then(
                if (interactive) Modifier.toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ) else Modifier
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .clip(CircleShape)
                .background(if (checked) p.text else p.trackOff),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(if (checked) p.bg else p.thumbOff),
            )
        }
    }
}

/** Thin monochrome slider with touch and accessibility progress semantics. */
@Composable
fun ExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val p = LocalExpressivePalette.current
    var widthPx by remember { mutableIntStateOf(0) }
    val fraction = if (valueRange.endInclusive > valueRange.start) {
        ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(ExpressiveMotion.microDurationMillis, easing = ExpressiveMotion.easeInOut),
        label = "expressiveSliderValue",
    )
    fun updateFromPosition(x: Float) {
        if (widthPx <= 0 || valueRange.endInclusive <= valueRange.start) return
        val nextFraction = (x / widthPx.toFloat()).coerceIn(0f, 1f)
        onValueChange(valueRange.start + (valueRange.endInclusive - valueRange.start) * nextFraction)
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(enabled, widthPx, valueRange) {
                if (enabled) detectTapGestures { updateFromPosition(it.x) }
            }
            .pointerInput(enabled, widthPx, valueRange) {
                if (enabled) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        updateFromPosition(change.position.x)
                    }
                }
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
                setProgress { requested ->
                    if (!enabled || valueRange.endInclusive <= valueRange.start) {
                        false
                    } else {
                        onValueChange(requested.coerceIn(valueRange.start, valueRange.endInclusive))
                        true
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            val centerY = size.height / 2f
            val startX = 4.dp.toPx()
            val endX = (size.width - 4.dp.toPx()).coerceAtLeast(startX)
            val activeX = startX + (endX - startX) * animatedFraction
            drawLine(
                color = p.trackOff,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = 2.dp.toPx(),
            )
            drawLine(
                color = p.text,
                start = Offset(startX, centerY),
                end = Offset(activeX, centerY),
                strokeWidth = 2.dp.toPx(),
            )
            drawCircle(
                color = if (enabled) p.text else p.dim,
                radius = 6.dp.toPx(),
                center = Offset(activeX, centerY),
            )
        }
    }
}

/** Flat input field shared by search, rename and text-entry dialogs. */
@Composable
fun ExpressiveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = TextStyle.Default,
) {
    val p = LocalExpressivePalette.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ExpressiveRadius.control))
            .background(p.surfaceHi)
            .padding(PaddingValues(horizontal = 12.dp, vertical = 12.dp)),
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        textStyle = textStyle.copy(color = p.text, fontSize = 14.sp),
        cursorBrush = SolidColor(p.text),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, color = p.faint, fontSize = 14.sp)
                }
                innerTextField()
            }
        },
    )
}

/** Small inline status label; use red only for an actual error or critical state. */
@Composable
fun ExpressiveStatus(
    text: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val p = LocalExpressivePalette.current
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(6.dp)) {
            drawCircle(if (isError) p.accent else p.dim)
        }
        Text(
            text = text,
            color = if (isError) p.accent else p.dim,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}


/** Compact monochrome checkbox shared by selection lists and dialogs. */
@Composable
fun ExpressiveCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val p = LocalExpressivePalette.current
    Canvas(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
    ) {
        val inset = 1.dp.toPx()
        val radius = 3.dp.toPx()
        if (checked) {
            drawRoundRect(
                color = p.text,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
            val start = Offset(size.width * 0.27f, size.height * 0.52f)
            val middle = Offset(size.width * 0.44f, size.height * 0.68f)
            val end = Offset(size.width * 0.75f, size.height * 0.34f)
            drawLine(p.bg, start, middle, strokeWidth = 1.8.dp.toPx())
            drawLine(p.bg, middle, end, strokeWidth = 1.8.dp.toPx())
        } else {
            drawRoundRect(
                color = if (enabled) p.dim else p.faint,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
    }
}
