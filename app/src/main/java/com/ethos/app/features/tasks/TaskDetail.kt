package com.ethos.app.features.tasks

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.tasks.domain.DeadlineKind
import com.ethos.app.core.tasks.domain.RepeatKind
import com.ethos.app.core.tasks.domain.RepeatRule
import com.ethos.app.core.tasks.domain.TaskFields
import com.ethos.app.core.tasks.domain.TaskList
import com.ethos.app.core.tasks.domain.TaskOccurrence
import com.ethos.app.core.tasks.domain.TaskStatus
import com.ethos.app.core.ui.components.DetailIconAction
import com.ethos.app.core.ui.components.DetailSummaryItem
import com.ethos.app.core.ui.components.ExpandableDescription
import com.ethos.app.core.ui.components.itemColors
import com.ethos.app.core.ui.format.EthosDates
import com.ethos.app.core.ui.format.label
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalDate

/** What the task detail popup can ask for; grouped because the popup has many actions (spec 021). */
data class TaskDetailActions(
    val close: () -> Unit,
    val complete: () -> Unit,
    val completeOn: (LocalDate) -> Unit,
    val notDone: () -> Unit,
    val undoResult: () -> Unit,
    val edit: () -> Unit,
    val delete: () -> Unit,
    val move: (String?) -> Unit,
    val check: (index: Int, checked: Boolean) -> Unit,
    val openReminders: () -> Unit,
    val openTimer: () -> Unit,
    val postpone: (LocalDate) -> Unit,
)

/**
 * Task details as a short popup (spec 021, "Mínimo"): summary, the two frequent actions and the other actions as
 * labelled icons. No history and no close button: it closes on outside tap or back.
 */
@Composable
fun TaskDetail(
    model: TaskDetailModel, lists: List<TaskList>, today: LocalDate, busy: Boolean,
    error: @Composable () -> Unit, actions: TaskDetailActions,
) {
    val o = model.occurrence
    var postponing by rememberSaveable(o.id) { mutableStateOf(false) }
    var pickingPostponeDate by rememberSaveable(o.id) { mutableStateOf(false) }
    var pickingCompletionDate by rememberSaveable(o.id) { mutableStateOf(false) }
    var choosingList by rememberSaveable(o.id) { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) actions.close() }, confirmButton = {},
        title = {
            Text(o.fields.title, textDecoration = if (model.isOpen) null else TextDecoration.LineThrough,
                color = if (model.isOpen) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                error()
                TaskSummaryLine(model, today, busy, actions.openTimer)
                if (o.fields.description.isNotEmpty()) ExpandableDescription(o.fields.description)
                if (o.checks.isNotEmpty()) TaskChecklist(o, busy || !model.isOpen, actions.check)
                if (model.isOpen) TaskMainButtons(model.canPostpone, busy, actions.complete,
                    onOtherDay = { pickingCompletionDate = true }, onPostpone = { postponing = true })
                else ResolvedStrip(o, today, busy, actions.undoResult)
                TaskActionIcons(model.isOpen, busy, TaskIconActions(actions.notDone, actions.edit,
                    actions.openReminders, { choosingList = true }, actions.delete))
            }
        })
    if (postponing) PostponeSheet(model, today, busy,
        onTomorrow = { postponing = false; actions.postpone(today.plusDays(1)) },
        onPick = { postponing = false; pickingPostponeDate = true }, onDismiss = { postponing = false })
    if (pickingPostponeDate) EthosDatePickerDialog(initial = today.plusDays(1), min = today,
        excluded = o.fields.date, confirmLabel = R.string.task_postpone,
        onConfirm = { pickingPostponeDate = false; actions.postpone(it) }, onDismiss = { pickingPostponeDate = false })
    if (pickingCompletionDate) EthosDatePickerDialog(initial = today, min = model.created, max = today,
        confirmLabel = R.string.task_complete,
        onConfirm = { pickingCompletionDate = false; actions.completeOn(it) },
        onDismiss = { pickingCompletionDate = false })
    if (choosingList) ListChoiceDialog(lists, o.fields.listId,
        onSelect = { choosingList = false; actions.move(it) }, onDismiss = { choosingList = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskSummaryLine(model: TaskDetailModel, today: LocalDate, busy: Boolean, openTimer: () -> Unit) {
    val o = model.occurrence
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically) {
        val due = o.dueDate?.let { EthosDates.short(it, today).label() } ?: stringResource(R.string.no_date)
        val dueWithTime = listOfNotNull(due, o.fields.minute?.let(EthosDates::time)).joinToString(" · ")
        DetailSummaryItem(R.drawable.ic_calendar_month,
            if (model.isOverdue) stringResource(R.string.task_overdue_suffix, dueWithTime) else dueWithTime,
            emphasized = model.isOverdue)
        val list = model.list
        DetailSummaryItem(if (list == null) R.drawable.ic_checklist else listIcons[list.icon].icon,
            list?.name ?: stringResource(R.string.inbox), tint = list?.let { itemColors[it.color] })
        model.repeat?.let { DetailSummaryItem(R.drawable.ic_repeat, repeatLabel(it)) }
        if (model.isOpen && model.reminderCount > 0) {
            val spoken = pluralStringResource(R.plurals.task_reminder_count, model.reminderCount, model.reminderCount)
            DetailSummaryItem(R.drawable.ic_notification_reminder, model.reminderCount.toString(), spoken = spoken)
        }
        val minutes = model.timerMinutes
        if (model.isOpen && minutes != null) {
            val spoken = pluralStringResource(R.plurals.task_open_timer, minutes, minutes)
            AssistChip(onClick = openTimer, enabled = !busy, modifier = Modifier.semantics { contentDescription = spoken },
                label = { Text(stringResource(R.string.task_timer_minutes, minutes)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_play_arrow), null, Modifier.size(18.dp)) })
        }
    }
}

@Composable
private fun repeatLabel(rule: RepeatRule): String = when (repeatChoiceOf(rule.kind)) {
    RepeatChoice.DAILY -> stringResource(R.string.repeat_every_day)
    RepeatChoice.WEEKLY -> stringResource(R.string.repeat_every_week)
    RepeatChoice.MONTHLY -> stringResource(R.string.repeat_every_month)
    RepeatChoice.INTERVAL, RepeatChoice.NONE ->
        pluralStringResource(R.plurals.task_repeat_interval, rule.interval, rule.interval)
}

@Composable
private fun TaskChecklist(o: TaskOccurrence, disabled: Boolean, check: (Int, Boolean) -> Unit) {
    Column {
        o.checks.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).toggleable(item.checked, enabled = !disabled,
                role = Role.Checkbox) { check(index, it) }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(item.checked, onCheckedChange = null, enabled = !disabled)
                Spacer(Modifier.width(12.dp))
                Text(item.label, style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null)
            }
        }
    }
}

/** "Concluir" with its "▾" for another day, and "Adiar" when the task has a specific day. */
@Composable
private fun TaskMainButtons(
    canPostpone: Boolean, busy: Boolean, onComplete: () -> Unit, onOtherDay: () -> Unit, onPostpone: () -> Unit,
) {
    var menu by rememberSaveable { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Button(onComplete, Modifier.weight(1f), enabled = !busy,
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp)) {
                Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.task_complete), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                val options = stringResource(R.string.task_complete_options)
                Button({ menu = true }, enabled = !busy, contentPadding = PaddingValues(horizontal = 10.dp),
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                    modifier = Modifier.semantics { contentDescription = options }) {
                    Icon(painterResource(R.drawable.ic_keyboard_arrow_down), null)
                }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.task_complete_other_day)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_calendar_month), null) },
                        onClick = { menu = false; onOtherDay() })
                }
            }
        }
        if (canPostpone) FilledTonalButton(onPostpone, Modifier.weight(1f), enabled = !busy) {
            Icon(painterResource(R.drawable.ic_today), null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.task_postpone), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ResolvedStrip(o: TaskOccurrence, today: LocalDate, busy: Boolean, undo: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(if (o.status == TaskStatus.DONE) R.drawable.ic_check else R.drawable.ic_close), null,
                Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            val status = if (o.status == TaskStatus.DONE) stringResource(R.string.task_done_status,
                (o.completedDate ?: today).let { EthosDates.short(it, today).label() })
                else stringResource(R.string.task_not_done_status)
            Text(status, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(undo, enabled = !busy) { Text(stringResource(R.string.undo)) }
        }
    }
}

private data class TaskIconActions(
    val notDone: () -> Unit, val edit: () -> Unit, val reminders: () -> Unit, val list: () -> Unit,
    val delete: () -> Unit,
)

@Composable
private fun TaskActionIcons(open: Boolean, busy: Boolean, actions: TaskIconActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (open) DetailIconAction(R.drawable.ic_close, R.string.not_done, !busy, actions.notDone,
            spoken = R.string.not_done_spoken)
        DetailIconAction(R.drawable.ic_edit, R.string.edit, !busy, actions.edit)
        DetailIconAction(R.drawable.ic_notification_reminder, R.string.reminders, !busy && open, actions.reminders)
        DetailIconAction(R.drawable.ic_format_list_bulleted, R.string.choose_list, !busy && open, actions.list)
        DetailIconAction(R.drawable.ic_delete, R.string.delete, !busy, actions.delete)
    }
}

/** "Amanhã" or "Escolher data", with the notes that explain what postponing does (spec 021). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostponeSheet(
    model: TaskDetailModel, today: LocalDate, busy: Boolean, onTomorrow: () -> Unit, onPick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val o = model.occurrence
    val due = o.fields.date ?: return
    val time = o.fields.minute?.let(EthosDates::time)
    val tomorrow = today.plusDays(1)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(stringResource(R.string.task_postpone_title, o.fields.title), Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleMedium)
            val current = EthosDates.short(due, today).label()
            Text(if (time == null) stringResource(R.string.task_postpone_current, current)
                else stringResource(R.string.task_postpone_current_time, current, time),
                Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val tomorrowText = listOfNotNull(EthosDates.shortWithWeekday(tomorrow, today), time).joinToString(" · ")
            PostponeOption(R.drawable.ic_today, R.string.task_postpone_tomorrow, tomorrowText,
                enabled = !busy && tomorrow != due, onClick = onTomorrow)
            PostponeOption(R.drawable.ic_calendar_month, R.string.task_postpone_pick,
                stringResource(R.string.task_postpone_pick_hint), enabled = !busy, onClick = onPick)
            if (model.repeat != null) PostponeNote(model.nextInSeries?.let {
                stringResource(R.string.task_postpone_series, EthosDates.shortWithWeekday(it, today))
            } ?: stringResource(R.string.task_postpone_series_unknown), MaterialTheme.colorScheme.surfaceContainerHighest)
            if (model.isOverdue) PostponeNote(stringResource(R.string.task_postpone_overdue,
                EthosDates.short(due, today).label().lowercase()), MaterialTheme.colorScheme.errorContainer)
        }
    }
}

@Composable
private fun PostponeOption(@DrawableRes icon: Int, @StringRes title: Int, subtitle: String, enabled: Boolean,
                           onClick: () -> Unit) {
    ListItem(headlineContent = { Text(stringResource(title)) }, supportingContent = { Text(subtitle) },
        leadingContent = { Icon(painterResource(icon), null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 8.dp))
}

@Composable
private fun PostponeNote(text: String, container: Color) {
    Surface(color = container, shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
        Text(text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ListChoiceDialog(
    lists: List<TaskList>, selected: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.task_move_title)) },
        confirmButton = {}, dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.picker_cancel)) } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ListChoiceRow(R.drawable.ic_checklist, null, stringResource(R.string.inbox), selected == null) {
                    onSelect(null) }
                lists.forEach { list ->
                    ListChoiceRow(listIcons[list.icon].icon, itemColors[list.color], list.name,
                        selected == list.id) { onSelect(list.id) }
                }
            }
        })
}

@Composable
private fun ListChoiceRow(@DrawableRes icon: Int, tint: Color?, name: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
        .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(painterResource(icon), null, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant)
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (selected) Icon(painterResource(R.drawable.ic_check), null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** "Adiada para amanhã · Desfazer" after a postpone (spec 021). */
@Composable
fun PostponedSnackbar(feedback: PostponeFeedback, today: LocalDate, undo: () -> Unit, dismiss: () -> Unit) {
    Snackbar(Modifier.padding(12.dp),
        action = { TextButton(undo) { Text(stringResource(R.string.undo)) } },
        dismissAction = { TextButton(dismiss) { Text(stringResource(R.string.close)) } },
    ) {
        Text(if (feedback.to == today.plusDays(1)) stringResource(R.string.task_postponed_tomorrow)
            else stringResource(R.string.task_postponed_to, EthosDates.shortWithWeekday(feedback.to, today)))
    }
}

@Preview
@Composable
private fun TaskDetailPreview() {
    val today = LocalDate.of(2026, 11, 16)
    val o = TaskOccurrence("p:1", "p", 1, TaskFields(title = "Preparar apresentação do trimestre",
        description = "Usar os números de outubro. Mandar para revisão antes de quinta.",
        deadline = DeadlineKind.DAY, date = today, minute = 18 * 60, checklist = listOf("Gráficos", "Slides")),
        today, today, today)
    val list = TaskList("w", "Trabalho", icon = 1)
    EthosTheme {
        TaskDetail(TaskDetailModel(o, isOpen = true, isOverdue = false, canPostpone = true,
            repeat = RepeatRule(RepeatKind.FIXED_INTERVAL, today, interval = 2), reminderCount = 2,
            timerMinutes = 45, list = list, nextInSeries = today.plusDays(2), created = today),
            listOf(list), today, busy = false, error = {},
            TaskDetailActions({}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}))
    }
}
