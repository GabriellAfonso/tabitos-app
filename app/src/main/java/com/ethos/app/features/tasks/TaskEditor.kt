package com.ethos.app.features.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.tasks.domain.*

val repeatLabels = listOf(R.string.no_repeat, R.string.repeat_daily, R.string.repeat_weekdays,
    R.string.repeat_weekly, R.string.repeat_custom, R.string.repeat_monthly, R.string.repeat_fixed,
    R.string.repeat_after)
val deadlineLabels = listOf(R.string.no_date, R.string.specific_day, R.string.this_week, R.string.work_week)
val colorLabels = listOf(R.string.color_sage, R.string.color_blue, R.string.color_purple,
    R.string.color_orange, R.string.color_rose, R.string.color_gray)
val iconLabels = listOf(R.string.icon_list, R.string.icon_work, R.string.icon_home,
    R.string.icon_study, R.string.icon_health, R.string.icon_star)

@Composable
fun ChoiceRow(label: Int, labels: List<Int>, selected: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, resource ->
            FilterChip(selected = selected == i, onClick = { onSelect(i) }, enabled = enabled,
                label = { Text(stringResource(resource)) }, modifier = Modifier.sizeIn(minHeight = 48.dp))
        }
    }
}

@Composable
fun Input(label: Int, value: String, enabled: Boolean = true, multiline: Boolean = false, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(stringResource(label)) }, enabled = enabled,
        singleLine = !multiline, modifier = Modifier.fillMaxWidth())
}

@Composable
fun TaskEditor(
    draft: TaskDraft, lists: List<TaskList>, busy: Boolean, error: @Composable () -> Unit,
    onChange: (TaskDraft) -> Unit, onDifficulty: (Int) -> Unit, onRepeat: (RepeatKind) -> Unit,
    onSave: () -> Unit, onClose: () -> Unit,
) {
    AlertDialog(onDismissRequest = { if (!busy) onClose() },
        title = { Text(stringResource(if (draft.editing) R.string.edit_task else R.string.new_task)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error()
                Input(R.string.title_field, draft.fields.title, !busy) {
                    onChange(draft.copy(fields = draft.fields.copy(title = it))) }
                Input(R.string.description_field, draft.fields.description, !busy, true) {
                    onChange(draft.copy(fields = draft.fields.copy(description = it))) }
                ListChoices(lists, draft.fields.listId, !busy) {
                    onChange(draft.copy(fields = draft.fields.copy(listId = it))) }
                ChoiceRow(R.string.difficulty, listOf(R.string.easy, R.string.medium, R.string.hard),
                    draft.fields.difficulty, !busy, onDifficulty)
                ChoiceRow(R.string.importance, listOf(R.string.low, R.string.medium, R.string.high),
                    draft.fields.importance, !busy) {
                    onChange(draft.copy(fields = draft.fields.copy(importance = it, importanceManual = true))) }
                com.ethos.app.core.ui.accounting.FinancialEstimate(draft.fields.difficulty, draft.fields.importance)
                val offset = if (draft.rule.kind == RepeatKind.NONE) 0 else 1
                ChoiceRow(R.string.deadline, deadlineLabels.drop(offset), draft.fields.deadline.ordinal - offset,
                    !busy) {
                    onChange(draft.copy(fields = draft.fields.copy(deadline = DeadlineKind.entries[it + offset]))) }
                if (draft.fields.deadline != DeadlineKind.NONE &&
                    (draft.rule.kind == RepeatKind.NONE || draft.editing)) {
                    Input(R.string.date_format, draft.dateText, !busy) { onChange(draft.copy(dateText = it)) }
                }
                if (draft.fields.deadline == DeadlineKind.DAY) {
                    Input(R.string.time_format, draft.timeText, !busy) { onChange(draft.copy(timeText = it)) }
                }
                Input(R.string.checklist_field, draft.checklistText, !busy, true) {
                    onChange(draft.copy(checklistText = it)) }
                ChoiceRow(R.string.repeat, repeatLabels, draft.rule.kind.ordinal, !busy) {
                    onRepeat(RepeatKind.entries[it]) }
                if (draft.rule.kind != RepeatKind.NONE) {
                    Text(stringResource(R.string.recurrence_day_hint))
                    if (!draft.editing) Input(R.string.start_date, draft.startText, !busy) {
                        onChange(draft.copy(startText = it)) }
                    if (draft.rule.kind == RepeatKind.WEEKLY || draft.rule.kind == RepeatKind.CUSTOM) {
                        val weekdays = listOf(R.string.monday, R.string.tuesday, R.string.wednesday,
                            R.string.thursday, R.string.friday, R.string.saturday, R.string.sunday)
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            weekdays.forEachIndexed { i, label ->
                                val bit = 1 shl i
                                FilterChip(selected = draft.rule.weekdays and bit != 0, enabled = !busy,
                                    onClick = { onChange(draft.copy(rule = draft.rule.copy(weekdays =
                                        if (draft.rule.kind == RepeatKind.WEEKLY) bit
                                        else draft.rule.weekdays xor bit))) },
                                    label = { Text(stringResource(label)) },
                                    modifier = Modifier.sizeIn(minHeight = 48.dp))
                            }
                        }
                    }
                    if (draft.rule.kind == RepeatKind.MONTHLY) {
                        Input(R.string.monthly_day, draft.monthDayText, !busy) {
                            onChange(draft.copy(monthDayText = it)) }
                    }
                    if (draft.rule.kind in listOf(RepeatKind.FIXED_INTERVAL, RepeatKind.AFTER_COMPLETION)) {
                        Input(R.string.interval_days, draft.intervalText, !busy) {
                            onChange(draft.copy(intervalText = it)) }
                    }
                    ChoiceRow(R.string.ending, listOf(R.string.never, R.string.until_date, R.string.after_count),
                        draft.ending, !busy) { onChange(draft.copy(ending = it)) }
                    if (draft.ending == 1) Input(R.string.final_date, draft.untilText, !busy) {
                        onChange(draft.copy(untilText = it)) }
                    if (draft.ending == 2) Input(R.string.occurrence_count, draft.countText, !busy) {
                        onChange(draft.copy(countText = it)) }
                }
                if (draft.editing && draft.recurringOriginal) {
                    ScopeChoice(draft.scope, !busy) { onChange(draft.copy(scope = it)) }
                }
                if (draft.editing) {
                    Text(stringResource(R.string.review_reschedule))
                    Row {
                        Checkbox(draft.reviewConfirmed, { onChange(draft.copy(reviewConfirmed = it)) }, enabled = !busy)
                        Text(stringResource(R.string.confirm_review))
                    }
                }
            }
        }, confirmButton = { TextButton(onSave, enabled = !busy) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClose, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
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
