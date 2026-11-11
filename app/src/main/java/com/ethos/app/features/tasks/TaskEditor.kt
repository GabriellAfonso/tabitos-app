package com.ethos.app.features.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ethos.app.R
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.ui.accounting.FinancialEstimate
import com.ethos.app.core.ui.format.EthosDates
import java.time.DayOfWeek
import java.time.LocalDate

val iconLabels = listOf(R.string.icon_list, R.string.icon_work, R.string.icon_home,
    R.string.icon_study, R.string.icon_health, R.string.icon_star)
private val weekdayLabels = listOf(R.string.monday, R.string.tuesday, R.string.wednesday,
    R.string.thursday, R.string.friday, R.string.saturday, R.string.sunday)
private val repeatChoiceLabels = listOf(R.string.no_repeat, R.string.repeat_every_day,
    R.string.repeat_every_week, R.string.repeat_every_month, R.string.repeat_every_x)

@Composable
fun ChoiceRow(label: Int, labels: List<Int>, selected: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    TextChoiceRow(label, labels.map { stringResource(it) }, selected, enabled, onSelect)
}

@Composable
private fun TextChoiceRow(
    label: Int, labels: List<String>, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit,
) {
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, text ->
            FilterChip(selected = selected == i, onClick = { onSelect(i) }, enabled = enabled,
                label = { Text(text) }, modifier = Modifier.sizeIn(minHeight = 48.dp))
        }
    }
}

@Composable
fun Input(
    label: Int, value: String, enabled: Boolean = true, multiline: Boolean = false, number: Boolean = false,
    change: (String) -> Unit,
) {
    OutlinedTextField(value, change, label = { Text(stringResource(label)) }, enabled = enabled,
        singleLine = !multiline, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default)
}

/**
 * Full-screen task editor (spec 013): quick capture fields first, the rest under "Mais opções".
 * Pickers write ISO text into the draft, so saving keeps the existing parsing and rules.
 */
@Composable
fun TaskEditor(
    draft: TaskDraft, lists: List<TaskList>, occurrence: TaskOccurrence?, today: LocalDate, busy: Boolean,
    error: @Composable () -> Unit, onChange: (TaskDraft) -> Unit, onDifficulty: (Int) -> Unit,
    onRepeat: (RepeatRule) -> Unit, onSave: () -> Unit, onClose: () -> Unit,
) {
    Dialog(onDismissRequest = { if (!busy) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClose, enabled = !busy) { Text(stringResource(R.string.cancel)) }
                    Text(stringResource(if (draft.editing) R.string.edit_task else R.string.new_task),
                        Modifier.weight(1f).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Button(onSave, enabled = !busy) { Text(stringResource(R.string.save)) }
                }
                HorizontalDivider()
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    error()
                    QuickFields(draft, lists, today, busy, onChange)
                    var expanded by rememberSaveable(draft.id) { mutableStateOf(hasAdvancedOptions(draft)) }
                    TextButton({ expanded = !expanded }, Modifier.testTag("more-options")) {
                        Text(stringResource(if (expanded) R.string.fewer_options else R.string.more_options) +
                            if (expanded) "  ▴" else "  ▾")
                    }
                    if (expanded) MoreOptions(draft, today, busy, onChange, onDifficulty, onRepeat)
                    if (draft.editing && draft.recurringOriginal) {
                        ScopeChoice(draft.scope, !busy) { onChange(draft.copy(scope = it)) }
                    }
                    if (needsReviewConfirmation(draft, occurrence, today)) {
                        Text(stringResource(R.string.review_reschedule), color = MaterialTheme.colorScheme.error)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(draft.reviewConfirmed, { onChange(draft.copy(reviewConfirmed = it)) },
                                enabled = !busy)
                            Text(stringResource(R.string.confirm_review))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickFields(
    draft: TaskDraft, lists: List<TaskList>, today: LocalDate, busy: Boolean, onChange: (TaskDraft) -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(draft.id) { if (!draft.editing) runCatching { focus.requestFocus() } }
    OutlinedTextField(draft.fields.title, { onChange(draft.copy(fields = draft.fields.copy(title = it))) },
        Modifier.fillMaxWidth().focusRequester(focus), enabled = !busy, singleLine = true,
        label = { Text(stringResource(R.string.title_field)) })
    val recurring = draft.rule.kind != RepeatKind.NONE
    val kinds = DeadlineKind.entries.filter { !recurring || it != DeadlineKind.NONE }
    val labels = kinds.map { kind -> when (kind) {
        DeadlineKind.NONE -> stringResource(R.string.no_date)
        DeadlineKind.DAY -> stringResource(R.string.specific_day)
        DeadlineKind.WEEK -> stringResource(R.string.this_week) + " · " +
            EthosDates.weekRange(today, DayOfWeek.SUNDAY)
        DeadlineKind.WORK_WEEK -> stringResource(R.string.work_week) + " · " +
            EthosDates.weekRange(today, DayOfWeek.FRIDAY)
    } }
    TextChoiceRow(R.string.deadline, labels, kinds.indexOf(draft.fields.deadline), !busy) { index ->
        val kind = kinds[index]
        onChange(if (kind == DeadlineKind.NONE) draft.copy(fields = draft.fields.copy(deadline = kind),
            dateText = "", timeText = "")
        else draft.copy(fields = draft.fields.copy(deadline = kind),
            dateText = draft.dateText.ifBlank { today.toString() },
            timeText = if (kind == DeadlineKind.DAY) draft.timeText else ""))
    }
    if (draft.fields.deadline != DeadlineKind.NONE) {
        // A new recurring task is anchored by its start date; editing moves this occurrence's date.
        if (recurring && !draft.editing) {
            DateField(R.string.start_date, draft.startText, today, !busy, { onChange(draft.copy(startText = it)) })
        } else {
            DateField(R.string.date_field, draft.dateText, today, !busy, { onChange(draft.copy(dateText = it)) })
        }
    }
    if (draft.fields.deadline == DeadlineKind.DAY) {
        TimeField(draft.timeText, !busy) { onChange(draft.copy(timeText = it)) }
    }
    ListChoices(lists, draft.fields.listId, !busy) { onChange(draft.copy(fields = draft.fields.copy(listId = it))) }
}

@Composable
private fun MoreOptions(
    draft: TaskDraft, today: LocalDate, busy: Boolean, onChange: (TaskDraft) -> Unit,
    onDifficulty: (Int) -> Unit, onRepeat: (RepeatRule) -> Unit,
) {
    Input(R.string.description_field, draft.fields.description, !busy, true) {
        onChange(draft.copy(fields = draft.fields.copy(description = it))) }
    ChecklistEditor(draft.fields.checklist, !busy) { onChange(draft.copy(fields = draft.fields.copy(checklist = it))) }
    ChoiceRow(R.string.difficulty, listOf(R.string.easy, R.string.medium, R.string.hard),
        draft.fields.difficulty, !busy, onDifficulty)
    ChoiceRow(R.string.importance, listOf(R.string.low, R.string.medium, R.string.high),
        draft.fields.importance, !busy) {
        onChange(draft.copy(fields = draft.fields.copy(importance = it, importanceManual = true))) }
    FinancialEstimate(draft.fields.difficulty, draft.fields.importance)
    RepeatOptions(draft, today, busy, onChange, onRepeat)
}

@Composable
private fun RepeatOptions(
    draft: TaskDraft, today: LocalDate, busy: Boolean, onChange: (TaskDraft) -> Unit, onRepeat: (RepeatRule) -> Unit,
) {
    val rule = draft.rule
    val choice = repeatChoiceOf(rule.kind)
    val start = runCatching { LocalDate.parse(draft.startText) }.getOrNull() ?: today
    ChoiceRow(R.string.repeat, repeatChoiceLabels, choice.ordinal, !busy) {
        val next = RepeatChoice.entries[it]
        // A new task keeps the day already picked when it becomes (or stops being) recurring.
        if (!draft.editing && (choice == RepeatChoice.NONE) != (next == RepeatChoice.NONE)) {
            onChange(if (next == RepeatChoice.NONE) draft.copy(dateText = draft.startText)
                else draft.copy(startText = draft.dateText.ifBlank { draft.startText }))
        }
        val anchor = if (choice == RepeatChoice.NONE) runCatching { LocalDate.parse(draft.dateText) }.getOrNull()
            ?: start else start
        onRepeat(ruleForChoice(rule, next, anchor)) }
    when (choice) {
        RepeatChoice.NONE -> return
        RepeatChoice.DAILY -> ChoiceRow(R.string.repeat_on_days,
            listOf(R.string.repeat_daily, R.string.repeat_weekdays), if (rule.kind == RepeatKind.WEEKDAYS) 1 else 0,
            !busy) { onRepeat(rule.copy(kind = if (it == 1) RepeatKind.WEEKDAYS else RepeatKind.DAILY)) }
        RepeatChoice.WEEKLY -> {
            Text(stringResource(R.string.repeat_on_days), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                weekdayLabels.forEachIndexed { i, label ->
                    FilterChip(selected = rule.weekdays and (1 shl i) != 0, enabled = !busy,
                        onClick = { val bits = toggleWeekday(rule.weekdays, i)
                            onRepeat(rule.copy(kind = weeklyKind(bits), weekdays = bits)) },
                        label = { Text(stringResource(label)) }, modifier = Modifier.sizeIn(minHeight = 48.dp))
                }
            }
        }
        RepeatChoice.MONTHLY -> Input(R.string.monthly_day, draft.monthDayText, !busy, number = true) {
            onChange(draft.copy(monthDayText = it)) }
        RepeatChoice.INTERVAL -> {
            Input(R.string.interval_days, draft.intervalText, !busy, number = true) {
                onChange(draft.copy(intervalText = it)) }
            ChoiceRow(R.string.repeat_count_from, listOf(R.string.repeat_from_planned, R.string.repeat_from_completion),
                if (rule.kind == RepeatKind.AFTER_COMPLETION) 1 else 0, !busy) {
                onRepeat(rule.copy(kind = if (it == 1) RepeatKind.AFTER_COMPLETION else RepeatKind.FIXED_INTERVAL)) }
        }
    }
    ChoiceRow(R.string.ending, listOf(R.string.never, R.string.until_date, R.string.after_count),
        draft.ending, !busy) { onChange(draft.copy(ending = it)) }
    if (draft.ending == 1) DateField(R.string.final_date, draft.untilText, today, !busy,
        { onChange(draft.copy(untilText = it)) }, min = start)
    if (draft.ending == 2) Input(R.string.occurrence_count, draft.countText, !busy, number = true) {
        onChange(draft.copy(countText = it)) }
}

@Composable
private fun ChecklistEditor(items: List<String>, enabled: Boolean, change: (List<String>) -> Unit) {
    var focusTarget by rememberSaveable { mutableIntStateOf(-1) }
    Text(stringResource(R.string.checklist), style = MaterialTheme.typography.labelLarge)
    items.forEachIndexed { index, item ->
        val name = item.ifBlank { stringResource(R.string.checklist_empty_item) }
        val focus = remember { FocusRequester() }
        LaunchedEffect(focusTarget, items.size) {
            if (focusTarget == index) { runCatching { focus.requestFocus() }; focusTarget = -1 }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(item, { text -> change(items.mapIndexed { i, old -> if (i == index) text else old }) },
                Modifier.weight(1f).focusRequester(focus).testTag("checklist:$index"), enabled = enabled,
                singleLine = true, label = { Text(stringResource(R.string.checklist_item, index + 1)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    if (item.isNotBlank()) { change(addChecklistItem(items, index)); focusTarget = index + 1 }
                }))
            SymbolButton("↑", stringResource(R.string.checklist_move_up, name), enabled && index > 0) {
                change(moveChecklistItem(items, index, -1)) }
            SymbolButton("↓", stringResource(R.string.checklist_move_down, name), enabled && index < items.lastIndex) {
                change(moveChecklistItem(items, index, 1)) }
            SymbolButton("✕", stringResource(R.string.checklist_remove, name), enabled) {
                change(removeChecklistItem(items, index)) }
        }
    }
    TextButton({ change(addChecklistItem(items)); focusTarget = items.size }, enabled = enabled) {
        Text("+ " + stringResource(R.string.checklist_add)) }
}

@Composable
private fun SymbolButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = description }) {
        Text(symbol, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ListChoices(lists: List<TaskList>, selected: String?, enabled: Boolean, onSelect: (String?) -> Unit) {
    Text(stringResource(R.string.choose_list), style = MaterialTheme.typography.labelLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected == null, { onSelect(null) }, enabled = enabled,
            label = { Text(stringResource(R.string.inbox)) }, modifier = Modifier.sizeIn(minHeight = 48.dp))
        lists.forEach { list ->
            FilterChip(selected == list.id, { onSelect(list.id) }, enabled = enabled,
                label = { Text(list.name) }, modifier = Modifier.sizeIn(minHeight = 48.dp))
        }
    }
}

@Composable
fun ScopeChoice(scope: ChangeScope, enabled: Boolean, change: (ChangeScope) -> Unit) {
    ChoiceRow(R.string.change_scope, listOf(R.string.only_this, R.string.this_and_future),
        scope.ordinal, enabled) { change(ChangeScope.entries[it]) }
}
