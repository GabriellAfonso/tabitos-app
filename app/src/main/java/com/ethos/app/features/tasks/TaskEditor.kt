package com.ethos.app.features.tasks

import androidx.annotation.DrawableRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.icons.AppIcon
import com.ethos.app.core.icons.AppIconTables
import com.ethos.app.core.tasks.domain.ChangeScope
import com.ethos.app.core.tasks.domain.DeadlineKind
import com.ethos.app.core.tasks.domain.RepeatKind
import com.ethos.app.core.tasks.domain.RepeatRule
import com.ethos.app.core.tasks.domain.TaskList
import com.ethos.app.core.tasks.domain.TaskOccurrence
import com.ethos.app.core.ui.accounting.FinancialEstimate
import com.ethos.app.core.ui.components.ChoiceSheet
import com.ethos.app.core.ui.components.DragHandle
import com.ethos.app.core.ui.components.EditorChip
import com.ethos.app.core.ui.components.EditorDescriptionLine
import com.ethos.app.core.ui.components.EditorMoreChip
import com.ethos.app.core.ui.components.EditorNameField
import com.ethos.app.core.ui.components.EditorToggleChip
import com.ethos.app.core.ui.components.EditorValueRow
import com.ethos.app.core.ui.components.FocusOnCreate
import com.ethos.app.core.ui.components.GrowingSheet
import com.ethos.app.core.ui.components.NumberStepper
import com.ethos.app.core.ui.components.RadioOptions
import com.ethos.app.core.ui.components.ReorderableColumn
import com.ethos.app.core.ui.components.SettingsSectionHeader
import com.ethos.app.core.ui.components.option
import com.ethos.app.core.ui.components.rememberExpandAction
import com.ethos.app.core.ui.format.EthosDates
import com.ethos.app.core.ui.format.label
import java.time.DayOfWeek
import java.time.LocalDate

/** Icons offered in the list editor's row; the rest are under "see more". */
internal val listPrincipalIcons = listOf(AppIcon.LIST, AppIcon.WORK, AppIcon.HOME, AppIcon.STUDY)

/** Drawing of the list's persisted icon index. */
internal val TaskList.iconDrawable: Int get() = AppIconTables.list[icon].option.icon
private val weekdayLabels = listOf(R.string.monday, R.string.tuesday, R.string.wednesday,
    R.string.thursday, R.string.friday, R.string.saturday, R.string.sunday)
// Indexed by the stored difficulty/importance value (0..2).
private val difficultyLabels = listOf(R.string.easy, R.string.medium, R.string.hard)
private val importanceLabels = listOf(R.string.low, R.string.medium, R.string.high)
private val levelValues = difficultyLabels.indices.toList()

@Composable
fun ChoiceRow(label: Int, labels: List<Int>, selected: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, text ->
            FilterChip(selected = selected == i, onClick = { onSelect(i) }, enabled = enabled,
                label = { Text(stringResource(text)) }, modifier = Modifier.sizeIn(minHeight = 48.dp))
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
 * Task editor sheet (spec 029): compact with value chips when creating, full screen with value rows after "Mais"
 * or when editing. Pickers write ISO text into the draft, so saving keeps the existing parsing and rules.
 * [onDifficulty] is null while game mode is off (spec 017): difficulty and its coin/XP estimate are hidden.
 */
@Composable
fun TaskEditor(
    draft: TaskDraft, lists: List<TaskList>, occurrence: TaskOccurrence?, today: LocalDate, busy: Boolean,
    error: @Composable () -> Unit, onChange: (TaskDraft) -> Unit, onDifficulty: ((Int) -> Unit)?,
    onRepeat: (RepeatRule) -> Unit, onSave: () -> Unit, onClose: () -> Unit, onNewList: () -> Unit = {},
) {
    var expanded by rememberSaveable(draft.id) { mutableStateOf(draft.editing) }
    var sheet by rememberSaveable(draft.id) { mutableStateOf<TaskEditorSheet?>(null) }
    val expand = rememberExpandAction { expanded = true }
    val open: (TaskEditorSheet) -> Unit = { sheet = it }
    val focus = remember { FocusRequester() }
    FocusOnCreate(draft.id, !draft.editing, focus)
    GrowingSheet(
        expanded = expanded,
        title = stringResource(if (draft.editing) R.string.edit_task else R.string.new_task),
        busy = busy, onDismiss = onClose, onCollapse = if (draft.editing) null else ({ expanded = false }),
        onSave = onSave,
        name = {
            EditorNameField(draft.fields.title, { onChange(draft.copy(fields = draft.fields.copy(title = it))) },
                stringResource(R.string.title_field), !busy, focus)
        },
    ) {
        error()
        if (!expanded) EditorDescriptionLine(draft.fields.description, !busy) { open(TaskEditorSheet.DESCRIPTION) }
        TaskChips(draft, lists, today, busy, onChange, open, onMore = expand.takeUnless { expanded })
        if (draft.showsTimedChecklistWarning) {
            Text(stringResource(R.string.timed_removes_checklist), color = MaterialTheme.colorScheme.error)
        }
        if (expanded) {
            TaskRows(draft, busy, gameMode = onDifficulty != null, open)
            if (onDifficulty != null) FinancialEstimate(draft.fields.difficulty, draft.fields.importance)
        }
        EditOnlyControls(draft, occurrence, today, busy, onChange)
        sheet?.let { current ->
            TaskChoiceSheet(current, draft, lists, today, busy, TaskSheetActions(onChange, onDifficulty, onRepeat,
                onNewList = { sheet = null; onNewList() }, close = { sheet = null }))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskChips(
    draft: TaskDraft, lists: List<TaskList>, today: LocalDate, busy: Boolean, onChange: (TaskDraft) -> Unit,
    open: (TaskEditorSheet) -> Unit, onMore: (() -> Unit)?,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SheetChip(TaskEditorSheet.WHEN, R.string.deadline, whenSummary(draft, today), R.drawable.ic_event, busy, open)
        SheetChip(TaskEditorSheet.REPEAT, R.string.repeat, repeatSummary(draft), R.drawable.ic_repeat, busy, open)
        SheetChip(TaskEditorSheet.LIST, R.string.choose_list, listName(lists, draft.fields.listId),
            R.drawable.ic_format_list_bulleted, busy, open)
        EditorToggleChip(stringResource(if (draft.timed) R.string.variant_timed else R.string.editor_timer_add),
            draft.timed, R.drawable.ic_notification_timer, enabled = !busy, onClick = {
                onChange(draft.copy(timed = !draft.timed, durationText = draft.durationText.ifBlank {
                    DEFAULT_MINUTES.toString() }))
            })
        if (draft.showsDurationChip) SheetChip(TaskEditorSheet.DURATION, R.string.editor_duration,
            stringResource(R.string.editor_minutes_value, minutesOf(draft)), R.drawable.ic_schedule, busy, open)
        onMore?.let { EditorMoreChip(it, enabled = !busy) }
    }
}

@Composable
private fun SheetChip(
    sheet: TaskEditorSheet, label: Int, value: String, @DrawableRes icon: Int, busy: Boolean,
    open: (TaskEditorSheet) -> Unit,
) {
    EditorChip(stringResource(label), value, icon, { open(sheet) }, Modifier.testTag("chip:${sheet.name}"),
        enabled = !busy)
}

@Composable
private fun TaskRows(draft: TaskDraft, busy: Boolean, gameMode: Boolean, open: (TaskEditorSheet) -> Unit) {
    SettingsSectionHeader(stringResource(R.string.more_options))
    if (draft.showsEndingRow) SheetRow(TaskEditorSheet.ENDING, R.string.ending, endingSummary(draft),
        R.drawable.ic_today, busy, open)
    SheetRow(TaskEditorSheet.DESCRIPTION, R.string.editor_description,
        draft.fields.description.ifBlank { stringResource(R.string.editor_none) }, R.drawable.ic_edit_note, busy, open)
    if (draft.showsChecklistRow) {
        val count = draft.fields.checklist.count { it.isNotBlank() }
        SheetRow(TaskEditorSheet.CHECKLIST, R.string.checklist,
            if (count == 0) stringResource(R.string.editor_none)
            else pluralStringResource(R.plurals.editor_checklist_items, count, count),
            R.drawable.ic_checklist, busy, open)
    }
    SheetRow(TaskEditorSheet.IMPORTANCE, R.string.importance,
        stringResource(importanceLabels[draft.fields.importance]), R.drawable.ic_flag, busy, open)
    if (gameMode) SheetRow(TaskEditorSheet.DIFFICULTY, R.string.difficulty,
        stringResource(difficultyLabels[draft.fields.difficulty]), R.drawable.ic_bolt, busy, open)
}

@Composable
private fun SheetRow(
    sheet: TaskEditorSheet, label: Int, value: String, @DrawableRes icon: Int, busy: Boolean,
    open: (TaskEditorSheet) -> Unit,
) {
    EditorValueRow(stringResource(label), value, icon, { open(sheet) }, Modifier.testTag("row:${sheet.name}"),
        enabled = !busy)
}

@Composable
private fun EditOnlyControls(
    draft: TaskDraft, occurrence: TaskOccurrence?, today: LocalDate, busy: Boolean, onChange: (TaskDraft) -> Unit,
) {
    if (draft.editing && draft.recurringOriginal) {
        ScopeChoice(draft.scope, !busy) { onChange(draft.copy(scope = it)) }
    }
    if (needsReviewConfirmation(draft, occurrence, today)) {
        Text(stringResource(R.string.review_reschedule), color = MaterialTheme.colorScheme.error)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(draft.reviewConfirmed, { onChange(draft.copy(reviewConfirmed = it)) }, enabled = !busy)
            Text(stringResource(R.string.confirm_review))
        }
    }
}

// Groups the callbacks the choice sheets need, so the sheet keeps a short parameter list.
private data class TaskSheetActions(
    val change: (TaskDraft) -> Unit, val difficulty: ((Int) -> Unit)?, val repeat: (RepeatRule) -> Unit,
    val onNewList: () -> Unit, val close: () -> Unit,
)

@Composable
private fun TaskChoiceSheet(
    sheet: TaskEditorSheet, draft: TaskDraft, lists: List<TaskList>, today: LocalDate, busy: Boolean,
    actions: TaskSheetActions,
) {
    val change = actions.change
    val title = when (sheet) {
        TaskEditorSheet.WHEN -> R.string.deadline
        TaskEditorSheet.REPEAT -> R.string.repeat
        TaskEditorSheet.ENDING -> R.string.ending
        TaskEditorSheet.LIST -> R.string.choose_list
        TaskEditorSheet.DURATION -> R.string.editor_duration
        TaskEditorSheet.DESCRIPTION -> R.string.editor_description
        TaskEditorSheet.CHECKLIST -> R.string.checklist
        TaskEditorSheet.IMPORTANCE -> R.string.importance
        TaskEditorSheet.DIFFICULTY -> R.string.difficulty
    }
    ChoiceSheet(stringResource(title), actions.close) {
        when (sheet) {
            TaskEditorSheet.WHEN -> WhenOptions(draft, today, busy, change)
            TaskEditorSheet.REPEAT -> RepeatOptions(draft, today, busy, change, actions.repeat)
            TaskEditorSheet.ENDING -> EndingOptions(draft, today, busy, change)
            TaskEditorSheet.LIST -> ListOptions(lists, draft.fields.listId, busy, actions.onNewList) {
                change(draft.copy(fields = draft.fields.copy(listId = it))) }
            TaskEditorSheet.DURATION -> NumberStepper(minutesOf(draft),
                { change(draft.copy(durationText = it.toString())) }, MINUTES_RANGE, stringResource(R.string.minutes),
                step = MINUTES_STEP, enabled = !busy)
            TaskEditorSheet.DESCRIPTION -> Input(R.string.description_field, draft.fields.description, !busy, true) {
                change(draft.copy(fields = draft.fields.copy(description = it))) }
            TaskEditorSheet.CHECKLIST -> ChecklistEditor(draft.fields.checklist, !busy) {
                change(draft.copy(fields = draft.fields.copy(checklist = it))) }
            TaskEditorSheet.IMPORTANCE -> RadioOptions(levelValues, draft.fields.importance, enabled = !busy,
                onSelect = { change(draft.copy(fields = draft.fields.copy(importance = it,
                    importanceManual = true))) },
                label = { stringResource(importanceLabels[it]) })
            TaskEditorSheet.DIFFICULTY -> RadioOptions(levelValues, draft.fields.difficulty, enabled = !busy,
                onSelect = { actions.difficulty?.invoke(it) }, label = { stringResource(difficultyLabels[it]) })
        }
    }
}

@Composable
private fun WhenOptions(draft: TaskDraft, today: LocalDate, busy: Boolean, change: (TaskDraft) -> Unit) {
    RadioOptions(deadlineKindsFor(draft), draft.fields.deadline, enabled = !busy,
        onSelect = { change(withDeadline(draft, it, today)) }, label = { deadlineLabel(it, today) })
    if (draft.fields.deadline != DeadlineKind.NONE) {
        if (draft.editsStartDate) {
            DateField(R.string.start_date, draft.startText, today, !busy, { change(draft.copy(startText = it)) })
        } else {
            DateField(R.string.date_field, draft.dateText, today, !busy, { change(draft.copy(dateText = it)) })
        }
    }
    if (draft.fields.deadline == DeadlineKind.DAY) TimeField(draft.timeText, !busy) {
        change(draft.copy(timeText = it)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatOptions(
    draft: TaskDraft, today: LocalDate, busy: Boolean, change: (TaskDraft) -> Unit, onRepeat: (RepeatRule) -> Unit,
) {
    val rule = draft.rule
    val choice = repeatChoiceOf(rule.kind)
    RadioOptions(RepeatChoice.entries, choice, enabled = !busy, label = { repeatChoiceLabel(it) }, onSelect = { next ->
        change(withRepeatChoiceDates(draft, next))
        onRepeat(ruleForRepeatChoice(draft, next, today))
    })
    when (choice) {
        RepeatChoice.NONE -> Unit
        RepeatChoice.DAILY -> RadioOptions(listOf(RepeatKind.DAILY, RepeatKind.WEEKDAYS), rule.kind, enabled = !busy,
            onSelect = { onRepeat(rule.copy(kind = it)) },
            label = { stringResource(if (it == RepeatKind.WEEKDAYS) R.string.repeat_weekdays else R.string.repeat_daily) })
        RepeatChoice.WEEKLY -> {
            Text(stringResource(R.string.repeat_on_days), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                weekdayLabels.forEachIndexed { i, label ->
                    FilterChip(selected = rule.weekdays and (1 shl i) != 0, enabled = !busy,
                        onClick = { val bits = toggleWeekday(rule.weekdays, i)
                            onRepeat(rule.copy(kind = weeklyKind(bits), weekdays = bits)) },
                        label = { Text(stringResource(label)) }, modifier = Modifier.sizeIn(minHeight = 48.dp))
                }
            }
        }
        RepeatChoice.MONTHLY -> NumberStepper(draft.monthDayText.toIntOrNull() ?: 1,
            { change(draft.copy(monthDayText = it.toString())) }, 1..31, "",
            prefix = stringResource(R.string.on_day), enabled = !busy)
        RepeatChoice.INTERVAL -> {
            NumberStepper(draft.intervalText.toIntOrNull() ?: 1, { change(draft.copy(intervalText = it.toString())) },
                1..365, stringResource(R.string.days), prefix = stringResource(R.string.every), enabled = !busy)
            Text(stringResource(R.string.repeat_count_from), style = MaterialTheme.typography.labelLarge)
            RadioOptions(listOf(RepeatKind.FIXED_INTERVAL, RepeatKind.AFTER_COMPLETION), rule.kind, enabled = !busy,
                onSelect = { onRepeat(rule.copy(kind = it)) }, label = {
                    stringResource(if (it == RepeatKind.AFTER_COMPLETION) R.string.repeat_from_completion
                        else R.string.repeat_from_planned) })
        }
    }
}

@Composable
private fun EndingOptions(draft: TaskDraft, today: LocalDate, busy: Boolean, change: (TaskDraft) -> Unit) {
    RadioOptions(listOf(ENDING_NEVER, ENDING_UNTIL_DATE, ENDING_AFTER_COUNT), draft.ending, enabled = !busy,
        onSelect = { change(draft.copy(ending = it)) }, label = { stringResource(endingLabel(it)) })
    if (draft.ending == ENDING_UNTIL_DATE) DateField(R.string.final_date, draft.untilText, today, !busy,
        { change(draft.copy(untilText = it)) }, min = repeatStart(draft, today))
    if (draft.ending == ENDING_AFTER_COUNT) NumberStepper(draft.countText.toIntOrNull() ?: 1,
        { change(draft.copy(countText = it.toString())) }, 1..999, stringResource(R.string.times_total),
        enabled = !busy)
}

@Composable
private fun ListOptions(
    lists: List<TaskList>, selected: String?, busy: Boolean, onNewList: () -> Unit, onSelect: (String?) -> Unit,
) {
    RadioOptions(listOf<String?>(null) + lists.map { it.id }, selected, enabled = !busy, onSelect = onSelect,
        label = { listName(lists, it) })
    // Creating the destination without leaving the task being written.
    TextButton(onNewList, enabled = !busy) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.new_list))
    }
}

@Composable
private fun ChecklistEditor(items: List<String>, enabled: Boolean, change: (List<String>) -> Unit) {
    var focusTarget by rememberSaveable { mutableIntStateOf(-1) }
    // Stable identities keep each field (and its drag animation) attached to its item; never stored.
    val ids = remember { mutableListOf<Long>() }
    val counter = remember { longArrayOf(0) }
    if (ids.size != items.size) { ids.clear(); repeat(items.size) { ids.add(counter[0]++) } }
    fun update(newItems: List<String>, newIds: List<Long>) {
        ids.clear(); ids.addAll(newIds); change(newItems)
    }
    fun add(after: Int) {
        update(addChecklistItem(items, after), ids.toMutableList().apply { add(after + 1, counter[0]++) })
        focusTarget = after + 1
    }
    val rows = items.indices.map { ids[it] to items[it] }
    ReorderableColumn(rows, { it.first }, enabled, onReorder = { moved ->
        update(moved.map { it.second }, moved.map { it.first })
    }) { (id, item), handle ->
        val index = rows.indexOfFirst { it.first == id }
        val name = item.ifBlank { stringResource(R.string.checklist_empty_item) }
        val focus = remember { FocusRequester() }
        LaunchedEffect(focusTarget, items.size) {
            if (focusTarget == index) { runCatching { focus.requestFocus() }; focusTarget = -1 }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            DragHandle(handle)
            OutlinedTextField(item, { text -> change(items.mapIndexed { i, old -> if (i == index) text else old }) },
                Modifier.weight(1f).focusRequester(focus).testTag("checklist:$index"), enabled = enabled,
                singleLine = true, label = { Text(stringResource(R.string.checklist_item, index + 1)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { if (item.isNotBlank()) add(index) }))
            IconActionButton(R.drawable.ic_close, stringResource(R.string.checklist_remove, name), enabled) {
                update(removeChecklistItem(items, index), ids.filterIndexed { i, _ -> i != index })
            }
        }
    }
    TextButton({ add(items.lastIndex) }, enabled = enabled) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.checklist_add))
    }
}

@Composable
private fun IconActionButton(@DrawableRes icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = description }) {
        Icon(painterResource(icon), contentDescription = null)
    }
}

@Composable
fun ScopeChoice(scope: ChangeScope, enabled: Boolean, change: (ChangeScope) -> Unit) {
    Text(stringResource(R.string.change_scope), style = MaterialTheme.typography.labelLarge)
    RadioOptions(ChangeScope.entries, scope, enabled = enabled, onSelect = change, label = {
        stringResource(if (it == ChangeScope.ONLY_THIS) R.string.only_this else R.string.this_and_future) })
}

@Composable
private fun whenSummary(draft: TaskDraft, today: LocalDate): String = when (draft.fields.deadline) {
    DeadlineKind.NONE -> stringResource(R.string.no_date)
    DeadlineKind.DAY -> {
        val text = if (draft.editsStartDate) draft.startText else draft.dateText
        val date = runCatching { LocalDate.parse(text) }.getOrNull()
        listOfNotNull(date?.let { EthosDates.short(it, today).label() } ?: stringResource(R.string.not_set),
            draft.timeText.ifBlank { null }).joinToString(" ")
    }
    DeadlineKind.WEEK -> stringResource(R.string.this_week)
    DeadlineKind.WORK_WEEK -> stringResource(R.string.work_week)
}

@Composable
private fun deadlineLabel(kind: DeadlineKind, today: LocalDate): String = when (kind) {
    DeadlineKind.NONE -> stringResource(R.string.no_date)
    DeadlineKind.DAY -> stringResource(R.string.specific_day)
    DeadlineKind.WEEK -> stringResource(R.string.this_week) + " · " + EthosDates.weekRange(today, DayOfWeek.SUNDAY)
    DeadlineKind.WORK_WEEK -> stringResource(R.string.work_week) + " · " +
        EthosDates.weekRange(today, DayOfWeek.FRIDAY)
}

@Composable
private fun repeatSummary(draft: TaskDraft): String {
    val choice = repeatChoiceOf(draft.rule.kind)
    if (choice != RepeatChoice.INTERVAL) return repeatChoiceLabel(choice)
    val days = draft.intervalText.toIntOrNull() ?: 1
    return pluralStringResource(R.plurals.task_repeat_interval, days, days)
}

@Composable
private fun repeatChoiceLabel(choice: RepeatChoice): String = stringResource(when (choice) {
    RepeatChoice.NONE -> R.string.no_repeat
    RepeatChoice.DAILY -> R.string.repeat_every_day
    RepeatChoice.WEEKLY -> R.string.repeat_every_week
    RepeatChoice.MONTHLY -> R.string.repeat_every_month
    RepeatChoice.INTERVAL -> R.string.repeat_every_x
})

private fun endingLabel(ending: Int): Int = when (ending) {
    ENDING_UNTIL_DATE -> R.string.until_date
    ENDING_AFTER_COUNT -> R.string.after_count
    else -> R.string.never
}

@Composable
private fun endingSummary(draft: TaskDraft): String = stringResource(endingLabel(draft.ending))

@Composable
private fun listName(lists: List<TaskList>, id: String?): String =
    lists.firstOrNull { it.id == id }?.name ?: stringResource(R.string.inbox)

private fun minutesOf(draft: TaskDraft): Int = draft.durationText.toIntOrNull() ?: DEFAULT_MINUTES

private const val DEFAULT_MINUTES = 25
private const val MINUTES_STEP = 5
private val MINUTES_RANGE = 5..600
