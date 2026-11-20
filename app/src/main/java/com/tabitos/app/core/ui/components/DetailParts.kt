package com.tabitos.app.core.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabitos.app.R
import com.tabitos.app.core.ui.theme.TabitosTheme

// Pieces shared by the task and habit detail popups (specs 021 and 023).

/**
 * Icon with its label under it, one of the equal-width actions at the bottom of a detail popup.
 * The label stays on one line and shrinks to fit, because narrow screens and large font scales would otherwise
 * break words in the middle ("Lembrete/s"); [spoken] lets TalkBack read a fuller name than the short label.
 */
@Composable
fun RowScope.DetailIconAction(
    @DrawableRes icon: Int, @StringRes label: Int, enabled: Boolean, onClick: () -> Unit,
    @StringRes spoken: Int? = null,
) {
    val color = if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
    val description = spoken?.let { stringResource(it) }
    val labelStyle = MaterialTheme.typography.labelSmall
    Column(Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) { description?.let { contentDescription = it } }
        .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(painterResource(icon), null, Modifier.size(22.dp), tint = color)
        Text(stringResource(label), style = labelStyle, textAlign = TextAlign.Center,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else color,
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = MinActionLabelSize, maxFontSize = labelStyle.fontSize,
                stepSize = ActionLabelSizeStep))
    }
}

private const val DisabledContentAlpha = 0.38f
private val MinActionLabelSize = 9.sp
private val ActionLabelSizeStep = 0.5.sp

/** Small icon and text of a popup summary line; [spoken] replaces what TalkBack reads when the text is terse. */
@Composable
fun DetailSummaryItem(
    @DrawableRes icon: Int, text: String, emphasized: Boolean = false, tint: Color? = null, spoken: String? = null,
) {
    val color = if (emphasized) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.semantics(mergeDescendants = true) { spoken?.let { contentDescription = it } },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(icon), null, Modifier.size(16.dp), tint = tint ?: color)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/** "Ver histórico ›" at the end of a detail popup: opens Estatísticas filtered by the item (spec 024). */
@Composable
fun DetailHistoryLink(enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick, enabled = enabled, contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
        Text(stringResource(R.string.view_history))
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, Modifier.size(18.dp))
    }
}

/**
 * Description limited to two lines until "Ver tudo" is tapped. The button only appears when the collapsed text was
 * actually cut, which depends on the width and font scale and so is only known after layout.
 */
@Composable
fun ExpandableDescription(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var truncated by remember(text) { mutableStateOf(false) }
    Column {
        Text(text, style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else CollapsedDescriptionLines, overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout -> if (!expanded) truncated = layout.hasVisualOverflow })
        if (!expanded && truncated) TextButton({ expanded = true },
            contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
            Text(stringResource(R.string.task_description_more))
        }
    }
}

private const val CollapsedDescriptionLines = 2

@Preview
@Composable
private fun DetailPartsPreview() {
    TabitosTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailSummaryItem(R.drawable.ic_repeat, "Todo dia")
            ExpandableDescription("Antes de dormir, longe do celular. Vale qualquer livro, menos os de trabalho.")
            Row(Modifier.fillMaxWidth()) {
                DetailIconAction(R.drawable.ic_edit, R.string.edit, enabled = true, onClick = {})
                DetailIconAction(R.drawable.ic_delete, R.string.delete, enabled = false, onClick = {})
            }
        }
    }
}
