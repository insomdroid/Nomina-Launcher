package com.insomdroid.nomina.ui.expressive

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import com.insomdroid.nomina.designsystem.theme.LauncherTextStyles
import androidx.compose.ui.unit.dp
import com.insomdroid.nomina.ui.LauncherIcons
import com.insomdroid.nomina.ui.launcherTechnicalFontFamily

@Composable
fun ExpressiveSectionTitle(text: String, color: androidx.compose.ui.graphics.Color = LocalExpressivePalette.current.text) {
    Text(
        text = text,
        color = color,
        style = LauncherTextStyles.expressiveSectionTitle.copy(fontFamily = launcherTechnicalFontFamily()),
        modifier = Modifier.padding(top = 24.dp, start = 2.dp, bottom = 12.dp),
    )
}

/**
 * A flat settings row with a quiet bottom divider. The full row remains tappable when [onClick]
 * is supplied; no setting state or callback is owned by this visual component.
 */
@Composable
fun ExpressiveCard(
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val p = LocalExpressivePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawBehind {
                val y = size.height - 0.5.dp.toPx()
                drawLine(
                    color = p.trackOff.copy(alpha = 0.72f),
                    start = Offset(4.dp.toPx(), y),
                    end = Offset(size.width - 4.dp.toPx(), y),
                    strokeWidth = 0.5.dp.toPx(),
                )
            }
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun ExpressiveActionRow(
    label: String,
    description: String,
    @DrawableRes trailingIcon: Int = LauncherIcons.ChevronRight,
    onClick: () -> Unit,
) {
    val p = LocalExpressivePalette.current
    ExpressiveCard(onClick = onClick) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = p.text, style = LauncherTextStyles.expressiveActionLabel)
            if (description.isNotEmpty()) {
                Text(description, color = p.dim, style = LauncherTextStyles.expressiveActionDescription.copy(fontFamily = launcherTechnicalFontFamily()))
            }
        }
        Icon(
            painter = painterResource(trailingIcon),
            contentDescription = null,
            tint = if (trailingIcon == LauncherIcons.Add) p.text else p.faint,
            modifier = Modifier.size(18.dp),
        )
    }
}
