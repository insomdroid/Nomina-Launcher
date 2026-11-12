package com.insomdroid.nomina.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.insomdroid.nomina.ui.LauncherIcons
import com.insomdroid.nomina.ui.expressive.LocalExpressivePalette

/**
 * A settings category row that opens its own page. The selected category displays
 * its existing settings content without changing any setting behavior.
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
    val state = stringResource(if (collapsed) R.string.settings_section_collapsed else R.string.settings_section_expanded)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 2.dp)
            .clickable(enabled = collapsed, role = Role.Button) { onToggle(false) }
            .semantics { stateDescription = state }
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
        val turn by animateFloatAsState(targetValue = if (collapsed) -90f else 0f, label = "chevron")
        Icon(
            painter = painterResource(LauncherIcons.ExpandMore),
            contentDescription = null,
            tint = p.dim,
            modifier = Modifier.size(20.dp).rotate(turn),
        )
    }
    AnimatedVisibility(
        visible = !collapsed,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(content = content)
    }
}
