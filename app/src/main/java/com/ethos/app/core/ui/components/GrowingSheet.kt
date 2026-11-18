package com.ethos.app.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ethos.app.R
import com.ethos.app.core.ui.theme.EthosTheme

const val EDITOR_SHEET_TAG = "editor-sheet"
const val EDITOR_SAVE_TAG = "editor-save"
const val EDITOR_NAME_TAG = "editor-name"
const val EDITOR_MORE_TAG = "editor-more"

/**
 * Editor sheet that grows (spec 029): [expanded] false is a bottom sheet over the current page with a round save
 * button; true fills the screen with a header and a text save button. Back and taps outside call [onDismiss], so
 * the caller decides whether a draft may be discarded. [onCollapse] null shows a close button instead (editing).
 */
@Composable
fun GrowingSheet(
    expanded: Boolean,
    title: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onCollapse: (() -> Unit)?,
    onSave: () -> Unit,
    name: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dismiss = { if (!busy) onDismiss() }
    Dialog(onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().then(if (expanded) Modifier else Modifier.statusBarsPadding()),
            contentAlignment = Alignment.BottomCenter) {
            // A sibling behind the surface, so taps on the sheet itself never reach it.
            Box(Modifier.matchParentSize().pointerInput(busy) { detectTapGestures { dismiss() } })
            Surface(
                Modifier.fillMaxWidth().then(if (expanded) Modifier.fillMaxSize() else Modifier)
                    .animateContentSize().testTag(EDITOR_SHEET_TAG).semantics { paneTitle = title },
                shape = if (expanded) RectangleShape else RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                if (expanded) ExpandedSheet(title, busy, dismiss, onCollapse, onSave, name, content)
                else CompactSheet(busy, onSave, name, content)
            }
        }
    }
}

@Composable
private fun CompactSheet(
    busy: Boolean, onSave: () -> Unit, name: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.navigationBarsPadding().imePadding().padding(start = 16.dp, end = 16.dp, top = 8.dp,
        bottom = 12.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp).size(width = 32.dp, height = 4.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = GRABBER_ALPHA), RoundedCornerShape(2.dp)))
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            name()
            content()
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            val save = stringResource(R.string.save)
            FilledIconButton(onSave, enabled = !busy, modifier = Modifier.size(48.dp).testTag(EDITOR_SAVE_TAG)
                .semantics { contentDescription = save }) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null)
            }
        }
    }
}

@Composable
private fun ExpandedSheet(
    title: String, busy: Boolean, dismiss: () -> Unit, onCollapse: (() -> Unit)?, onSave: () -> Unit,
    name: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            val leading = if (onCollapse != null) R.drawable.ic_keyboard_arrow_down to R.string.editor_collapse
                else R.drawable.ic_close to R.string.close
            val description = stringResource(leading.second)
            IconButton(onCollapse ?: dismiss, enabled = !busy,
                modifier = Modifier.semantics { contentDescription = description }) {
                Icon(painterResource(leading.first), contentDescription = null)
            }
            Text(title, Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() },
                style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            name()
            content()
            Spacer(Modifier.size(8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            Button(onSave, enabled = !busy, modifier = Modifier.testTag(EDITOR_SAVE_TAG)) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

/** Large, borderless name field at the top of the editor sheet; [focus] lets a new item start typing at once. */
@Composable
fun EditorNameField(
    value: String, onValueChange: (String) -> Unit, label: String, enabled: Boolean, focus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    TextField(value, onValueChange, modifier.fillMaxWidth().focusRequester(focus).testTag(EDITOR_NAME_TAG),
        enabled = enabled, singleLine = true, label = { Text(label) },
        textStyle = MaterialTheme.typography.titleLarge,
        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent))
}

/** Asks for focus and the keyboard when a new item's editor opens; editing keeps the keyboard closed. */
@Composable
fun FocusOnCreate(key: Any, creating: Boolean, focus: FocusRequester) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(key) {
        // The dialog window may not accept focus yet; the field then simply starts unfocused.
        if (creating) runCatching { focus.requestFocus(); keyboard?.show() }
    }
}

/** Closes the keyboard before the sheet grows to show the remaining options. */
@Composable
fun rememberExpandAction(onExpand: () -> Unit): () -> Unit {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    return { focusManager.clearFocus(); keyboard?.hide(); onExpand() }
}

/** "Mais" chip of the compact sheet. */
@Composable
fun EditorMoreChip(onClick: () -> Unit, enabled: Boolean = true) {
    AssistChip(onClick, label = { Text(stringResource(R.string.editor_more)) },
        modifier = Modifier.sizeIn(minHeight = 48.dp).testTag(EDITOR_MORE_TAG), enabled = enabled,
        leadingIcon = { Icon(painterResource(R.drawable.ic_more_horiz), contentDescription = null,
            Modifier.size(AssistChipDefaults.IconSize)) })
}

/** Description preview in the compact sheet; opens the description sheet. */
@Composable
fun EditorDescriptionLine(description: String, enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.editor_description)
    TextButton(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(description.ifBlank { label }, Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Quick value chip: shows [value] and is read as "[label]: [value]". [tint] colors the icon (habit color). */
@Composable
fun EditorChip(
    label: String,
    value: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val description = stringResource(R.string.editor_chip_value, label, value)
    AssistChip(onClick, label = { Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier.sizeIn(minHeight = 48.dp).clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick { if (enabled) onClick(); enabled }
        },
        enabled = enabled,
        leadingIcon = { Icon(painterResource(icon), contentDescription = null,
            Modifier.size(AssistChipDefaults.IconSize), tint = tint) })
}

/** On/off chip for a variant, such as the timer; [text] already says the current state. */
@Composable
fun EditorToggleChip(
    text: String, selected: Boolean, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(selected, onClick, label = { Text(text) }, modifier = modifier.sizeIn(minHeight = 48.dp),
        enabled = enabled,
        leadingIcon = { Icon(painterResource(icon), contentDescription = null,
            Modifier.size(FilterChipDefaults.IconSize)) })
}

/** Choice sheet opened from a chip or row; "Pronto" and dismissing both close it, keeping what was chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            content()
            TextButton(onDismiss, Modifier.align(Alignment.End)) { Text(stringResource(R.string.editor_done)) }
        }
    }
}

/** Single choice as radio rows; [label] names each option. */
@Composable
fun <T> RadioOptions(
    options: List<T>, selected: T?, onSelect: (T) -> Unit, label: @Composable (T) -> String,
    enabled: Boolean = true,
) {
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            Row(Modifier.fillMaxWidth().sizeIn(minHeight = 52.dp)
                .selectable(option == selected, enabled = enabled, role = Role.RadioButton) { onSelect(option) }
                .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RadioButton(option == selected, onClick = null, enabled = enabled)
                Text(label(option), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Row of the expanded sheet: label, current value and a chevron; opens that field's choice sheet. */
@Composable
fun EditorValueRow(
    label: String, value: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(modifier) { SettingsLinkRow(label, icon, onClick, supporting = value, enabled = enabled) }
}

private const val GRABBER_ALPHA = 0.6f

@Preview
@Composable
private fun EditorPartsPreview() {
    EthosTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EditorChip("Prazo", "Amanhã 09:00", R.drawable.ic_event, onClick = {})
                    EditorToggleChip("Com cronômetro", selected = true, R.drawable.ic_notification_timer,
                        onClick = {})
                }
                RadioOptions(listOf("Baixa", "Média", "Alta"), selected = "Média", onSelect = {},
                    label = { it })
                EditorValueRow("Importância", "Média", R.drawable.ic_flag, onClick = {})
            }
        }
    }
}
