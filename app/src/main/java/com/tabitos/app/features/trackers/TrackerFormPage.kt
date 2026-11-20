package com.tabitos.app.features.trackers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.trackers.domain.QuestionType
import com.tabitos.app.core.trackers.domain.TrackerOption
import com.tabitos.app.core.trackers.domain.TrackerQuestion
import com.tabitos.app.core.trackers.domain.TrackerRules
import com.tabitos.app.core.ui.components.DragHandle
import com.tabitos.app.core.ui.components.NumberStepper
import com.tabitos.app.core.ui.components.ReorderableColumn
import com.tabitos.app.core.ui.components.ScrollableTimeField
import com.tabitos.app.core.ui.components.SegmentedControl
import com.tabitos.app.core.ui.theme.TabitosTheme

/** Callbacks of the question editor; [add] returns the new question id so it opens expanded. */
data class TrackerFormActions(
    val change: (TrackerFormDraft) -> Unit,
    val add: (QuestionKind) -> String,
    val newOption: () -> TrackerOption,
    val save: () -> Unit,
    val close: () -> Unit,
    val retry: () -> Unit,
)

private val scaleRange = -TrackerRules.SCALE_LIMIT..TrackerRules.SCALE_LIMIT

/** Full-screen question editor (spec 034): one card open at a time, types added from the bottom bar. */
@Composable
fun TrackerFormPage(draft: TrackerFormDraft, habitName: String, state: TrackerUiState, actions: TrackerFormActions) {
    val enabled = !state.busy
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.problemQuestion) {
        state.problemQuestion?.takeIf { id -> draft.questions.any { it.id == id } }?.let { open = it }
    }
    TrackerFullScreen(
        title = stringResource(if (draft.editing) R.string.tracker_form_edit else R.string.tracker_form_new),
        subtitle = habitName, busy = state.busy, onClose = actions.close, closeIcon = R.drawable.ic_close,
        closeLabel = R.string.close,
        actions = {
            Button(actions.save, enabled = enabled, modifier = Modifier.padding(end = 8.dp)) {
                Text(stringResource(R.string.save))
            }
        },
        bottomBar = { AddQuestionBar(enabled) { kind -> open = actions.add(kind) } },
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TrackerError(state.storageError, state.problem, state.problemQuestion, actions.retry)
            if (draft.editing) Text(stringResource(R.string.tracker_versions_hint),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ModeRow(draft.multiple, enabled) { actions.change(draft.copy(multiple = it)) }
            if (draft.questions.isEmpty()) Text(stringResource(R.string.tracker_form_empty),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ReorderableColumn(draft.questions, { it.id }, enabled,
                onReorder = { actions.change(draft.copy(questions = it)) }) { q, handle ->
                QuestionCard(q, draft.questions.indexOf(q) + 1, open == q.id, handle, enabled,
                    toggle = { open = if (open == q.id) null else q.id },
                    change = { updated -> actions.change(draft.copy(questions = draft.questions.map {
                        if (it.id == q.id) updated else it })) },
                    setPrimary = { actions.change(draft.copy(questions = draft.questions.withPrimary(q.id, it))) },
                    remove = { actions.change(draft.copy(questions = draft.questions - q)) },
                    newOption = actions.newOption)
            }
            ScrollableTimeField(R.string.tracker_reminder, draft.reminder, enabled, optional = true,
                onChange = { actions.change(draft.copy(reminder = it)) })
            Text(stringResource(R.string.tracker_reminder_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModeRow(multiple: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.tracker_mode), style = MaterialTheme.typography.titleSmall)
            SegmentedControl(listOf(false, true), multiple, label = {
                stringResource(if (it) R.string.tracker_mode_multiple else R.string.tracker_mode_single)
            }, onSelect = { if (enabled) change(it) })
        }
    }
}

@Composable
private fun RowScope.AddQuestionBar(enabled: Boolean, add: (QuestionKind) -> Unit) {
    Text(stringResource(R.string.tracker_add), style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        QuestionKind.entries.forEach { kind ->
            AssistChip(onClick = { add(kind) }, enabled = enabled, label = { Text(stringResource(kind.shortLabel)) },
                modifier = Modifier.testTag(addQuestionTag(kind)))
        }
    }
}

fun addQuestionTag(kind: QuestionKind) = "tracker-add-${kind.name}"

private val QuestionKind.shortLabel: Int get() = when (this) {
    QuestionKind.SCALE -> R.string.tracker_kind_scale
    QuestionKind.SINGLE -> R.string.tracker_kind_single
    QuestionKind.MULTIPLE -> R.string.tracker_kind_multiple
    QuestionKind.TEXT -> R.string.tracker_kind_text
}

@Composable
private fun QuestionCard(
    q: TrackerQuestion, number: Int, expanded: Boolean, handle: Modifier, enabled: Boolean, toggle: () -> Unit,
    change: (TrackerQuestion) -> Unit, setPrimary: (Boolean) -> Unit, remove: () -> Unit,
    newOption: () -> TrackerOption,
) {
    val border = if (expanded) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else CardDefaults.outlinedCardBorder()
    // Only a closed card opens on tap; an open one keeps taps for its fields and closes from its arrow.
    val card = if (expanded) Modifier.fillMaxWidth()
        else Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = toggle)
    OutlinedCard(modifier = card, border = border) {
        Column(Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = if (expanded) 12.dp else 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QuestionHeader(q, number, expanded, handle, enabled, toggle)
            if (!expanded) return@Column
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(q.title, { change(q.copy(title = it)) }, Modifier.fillMaxWidth(), enabled = enabled,
                    label = { Text(stringResource(R.string.tracker_question_title)) })
                SegmentedControl(QuestionKind.entries, q.kind, label = { stringResource(it.shortLabel) },
                    onSelect = { if (enabled) change(q.withKind(it, newOption)) })
                when (q.type) {
                    QuestionType.SCALE -> ScaleFields(q, enabled, change, setPrimary)
                    QuestionType.CHOICE -> OptionFields(q, enabled, change, newOption)
                    QuestionType.TEXT -> Unit
                }
                LabeledSwitch(R.string.tracker_required, q.required, enabled) { change(q.copy(required = it)) }
                TextButton(remove, enabled = enabled) {
                    Text(stringResource(R.string.tracker_remove_question), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun QuestionHeader(
    q: TrackerQuestion, number: Int, expanded: Boolean, handle: Modifier, enabled: Boolean, toggle: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DragHandle(handle)
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(q.title.ifBlank { stringResource(R.string.tracker_question_untitled) },
                style = MaterialTheme.typography.titleSmall,
                color = if (q.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.tracker_question_number, number, questionSummary(q)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val description = stringResource(if (expanded) R.string.tracker_collapse_question
            else R.string.tracker_expand_question)
        IconButton(toggle, enabled = enabled, modifier = Modifier.semantics { contentDescription = description }) {
            Icon(painterResource(R.drawable.ic_keyboard_arrow_down), contentDescription = null,
                Modifier.rotate(if (expanded) HALF_TURN_DEGREES else 0f))
        }
    }
}

private const val HALF_TURN_DEGREES = 180f

@Composable
private fun ScaleFields(
    q: TrackerQuestion, enabled: Boolean, change: (TrackerQuestion) -> Unit, setPrimary: (Boolean) -> Unit,
) {
    NumberStepper(q.min, { change(q.copy(min = it)) }, scaleRange.first..(q.max - 1),
        suffix = "", prefix = stringResource(R.string.tracker_scale_min), enabled = enabled)
    NumberStepper(q.max, { change(q.copy(max = it)) }, (q.min + 1)..scaleRange.last,
        suffix = "", prefix = stringResource(R.string.tracker_scale_max), enabled = enabled)
    OutlinedTextField(q.minLabel, { change(q.copy(minLabel = it)) }, Modifier.fillMaxWidth(), enabled = enabled,
        singleLine = true, label = { Text(stringResource(R.string.tracker_scale_label_for, q.min)) })
    OutlinedTextField(q.maxLabel, { change(q.copy(maxLabel = it)) }, Modifier.fillMaxWidth(), enabled = enabled,
        singleLine = true, label = { Text(stringResource(R.string.tracker_scale_label_for, q.max)) })
    LabeledSwitch(R.string.tracker_primary, q.primary, enabled, setPrimary)
}

@Composable
private fun OptionFields(
    q: TrackerQuestion, enabled: Boolean, change: (TrackerQuestion) -> Unit, newOption: () -> TrackerOption,
) {
    ReorderableColumn(q.options, { it.id }, enabled, onReorder = { change(q.copy(options = it)) }) { o, handle ->
        OptionRow(o, handle, enabled,
            update = { value -> change(q.copy(options = q.options.map { if (it.id == o.id) value else it })) },
            remove = { change(q.copy(options = q.options - o)) })
    }
    TextButton({ change(q.copy(options = q.options + newOption())) }, enabled = enabled) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
        Text(stringResource(R.string.tracker_add_option), Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun OptionRow(
    o: TrackerOption, handle: Modifier, enabled: Boolean, update: (TrackerOption) -> Unit, remove: () -> Unit,
) {
    var describing by rememberSaveable(o.id) { mutableStateOf(o.description.isNotBlank()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DragHandle(handle)
            OutlinedTextField(o.label, { update(o.copy(label = it)) }, Modifier.weight(1f), enabled = enabled,
                singleLine = true, label = { Text(stringResource(R.string.tracker_option)) })
            val removeLabel = stringResource(R.string.tracker_remove_option)
            IconButton(remove, enabled = enabled, modifier = Modifier.semantics { contentDescription = removeLabel }) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = null)
            }
        }
        if (describing) OutlinedTextField(o.description, { update(o.copy(description = it)) },
            Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp), enabled = enabled, singleLine = true,
            label = { Text(stringResource(R.string.tracker_option_description)) })
        else TextButton({ describing = true }, enabled = enabled, modifier = Modifier.padding(start = 40.dp)) {
            Text(stringResource(R.string.tracker_option_add_description))
        }
    }
}

@Composable
private fun LabeledSwitch(label: Int, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, enabled, Role.Switch, onValueChange = change)
        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f))
        Switch(checked, null, enabled = enabled)
    }
}

@Preview
@Composable
private fun TrackerFormPreview() {
    val questions = listOf(
        TrackerQuestion("u", QuestionType.SCALE, "Como está sua energia?", required = true, primary = true,
            minLabel = "Esgotado", maxLabel = "Muito disposto"),
        TrackerQuestion("c", QuestionType.CHOICE, "O que atrapalhou hoje?", multiple = true,
            options = listOf(TrackerOption("a", "Sono ruim"), TrackerOption("b", "Trabalho"))))
    TabitosTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                questions.forEachIndexed { i, q ->
                    QuestionCard(q, i + 1, expanded = i == 1, handle = Modifier, enabled = true, toggle = {},
                        change = {}, setPrimary = {}, remove = {}, newOption = { TrackerOption("n", "") })
                }
            }
        }
    }
}
