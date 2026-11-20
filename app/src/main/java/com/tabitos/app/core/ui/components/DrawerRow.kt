package com.tabitos.app.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.ui.theme.TabitosTheme

/** Compact drawer rows (48 dp instead of Material's 56) fit more lists before scrolling; still a full touch target. */
private val DRAWER_ROW_MIN_HEIGHT = 48.dp

/**
 * One row of the ☰ menu: pages, lists, "Criar lista" and Configurações share it so every label starts in the
 * same column. Gestures and semantics come from [modifier] (a tap, or the hold-and-drag of a list); the row only
 * draws. [tint] colors a list's icon; [badge] shows a count such as pending reviews.
 */
@Composable
fun DrawerRow(
    label: String, @DrawableRes icon: Int, selected: Boolean, modifier: Modifier = Modifier,
    tint: Color? = null, badge: Int? = null,
) {
    val colors = MaterialTheme.colorScheme
    val content = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().heightIn(min = DRAWER_ROW_MIN_HEIGHT).clip(CircleShape).then(modifier)
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .padding(start = 16.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint ?: content)
        Text(label, Modifier.weight(1f).padding(vertical = 8.dp), style = MaterialTheme.typography.labelLarge,
            color = content)
        badge?.let { Badge(containerColor = colors.error, contentColor = colors.onError) { Text(it.toString()) } }
    }
}

@Preview(showBackground = true)
@Composable
private fun DrawerRowPreview() {
    TabitosTheme {
        DrawerRow("Revisar pendências", R.drawable.ic_pending_actions, selected = true, badge = 2)
    }
}
