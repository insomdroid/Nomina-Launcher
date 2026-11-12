package com.insomdroid.nomina.feature.settings

import androidx.annotation.DrawableRes
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
import com.insomdroid.nomina.ui.expressive.LocalExpressivePalette

/**
 * Keeps the established Settings section heading appearance inside a category page.
 * The category page itself is the navigation boundary, so its content is shown directly
 * without adding another collapsible layer.
 */
@Composable
internal fun CollapsibleSection(
    title: String,
    @DrawableRes icon: Int,
    collapsed: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalExpressivePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 2.dp)
            .padding(horizontal = 6.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = p.dim,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title.uppercase(),
            color = p.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.15.sp,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
    }
    Column(content = content)
}
