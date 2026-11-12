package com.insomdroid.nomina.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import com.insomdroid.nomina.ui.LauncherIcons
import com.insomdroid.nomina.ui.expressive.LocalExpressivePalette
import com.insomdroid.nomina.ui.launcherTechnicalFontFamily

/**
 * A settings category row that opens its own page.
 * The category list shows navigation rows; an opened page shows its settings directly.
 */
@Composable
internal fun CollapsibleSection(
    title: String,
    @DrawableRes icon: Int,
    visible: Boolean = true,
    collapsed: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    val p = LocalExpressivePalette.current

    if (collapsed) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 0.dp)
                .clickable(role = Role.Button) { onToggle(false) }
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = p.text,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = title.uppercase(),
                color = p.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                fontFamily = launcherTechnicalFontFamily(),
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Icon(
                painter = painterResource(LauncherIcons.ChevronRight),
                contentDescription = null,
                tint = p.text,
                modifier = Modifier.size(20.dp),
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            content = content,
        )
    }
}
