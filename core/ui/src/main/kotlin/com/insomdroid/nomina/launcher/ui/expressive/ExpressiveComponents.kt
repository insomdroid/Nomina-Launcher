package com.insomdroid.nomina.ui.expressive

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.insomdroid.nomina.ui.LauncherIcons
import androidx.compose.ui.graphics.Color

@Composable
fun ExpressiveSectionTitle(text: String, color: Color = LocalExpressivePalette.current.text) {
    Text(
        text = text, color = color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
        letterSpacing = 0.sp,
        modifier = Modifier.padding(top = 22.dp, start = 2.dp, bottom = 10.dp),
    )
}

@Composable
fun ExpressiveCard(
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val p = LocalExpressivePalette.current
    Surface(
        color = p.surfaceHi,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, p.trackOff.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(bottom = 6.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Monochrome title + dim description, with a trailing vector icon (chevron = opens a sub-screen, add =
 *  "+"); the add icon is accented, the chevron is faint. */
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
            Text(label, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(description, color = p.dim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Icon(
            painter = painterResource(trailingIcon),
            contentDescription = null,
            tint = if (trailingIcon == LauncherIcons.Add) p.text else p.faint,
            modifier = Modifier.size(18.dp),
        )
    }
}
