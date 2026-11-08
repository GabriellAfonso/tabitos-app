package com.ethos.app.features.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.ui.reminders.LocalOpenReminders
import com.ethos.app.core.reminders.domain.ReminderKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TaskPage { LIST, TODAY, REVIEW, CALENDAR }
private val listColors = listOf(Color(0xFF54745C), Color(0xFF426B96), Color(0xFF79568E),
    Color(0xFFA45B25), Color(0xFFA04D68), Color(0xFF657078))
private val listSymbols = listOf("≡", "▣", "⌂", "▤", "♡", "☆")

@Composable
fun TaskError(state: TasksUiState, retry: () -> Unit, dismiss: () -> Unit) {
    val resource = when {
        state.storageError -> R.string.task_storage_error
        state.error == TaskProblem.INVALID_DATE -> R.string.task_invalid_date
        state.error == TaskProblem.REVIEW_REQUIRED -> R.string.task_review_required
        state.error == TaskProblem.DEPENDENT_ACTIONS -> R.string.task_dependent_actions
        state.error == TaskProblem.NOT_OPEN -> R.string.task_not_open
        state.error != null -> R.string.task_invalid_input
        else -> null
    }
    if (resource != null) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(resource), color = MaterialTheme.colorScheme.error)
                Row {
                    if (state.storageError) TextButton(retry) { Text(stringResource(R.string.retry)) }
                    TextButton(dismiss) { Text(stringResource(R.string.close)) }
                }
            }
        }
    }
}

@Composable
fun TaskDrawer(
    state: TasksUiState, onSelect: (String?) -> Unit, onNew: () -> Unit, onManage: () -> Unit,
) {
    TextButton({ onSelect(null) }, enabled = !state.busy) { Text(stringResource(R.string.inbox)) }
    state.snapshot.activeLists.forEach { list ->
        TextButton({ onSelect(list.id) }, enabled = !state.busy) {
            Text(listSymbols[list.icon], color = listColors[list.color], modifier = Modifier.padding(end = 8.dp))
            Text(list.name)
        }
    }
    TextButton(onNew, enabled = !state.busy) { Text(stringResource(R.string.new_list)) }
    TextButton(onManage, enabled = !state.busy) { Text(stringResource(R.string.manage_lists)) }
}

@Composable
fun TasksContent(
    state: TasksUiState, page: TaskPage, filter: Int, onNew: () -> Unit, onOpen: (String) -> Unit,
    onAction: (TaskAction) -> Unit, onManage: () -> Unit, onRetry: () -> Unit, onDismissError: () -> Unit,
    extraItems: (LazyListScope.() -> Unit)? = null,
    header: @Composable () -> Unit = {},
) {
    var collapsed by rememberSaveable(page, state.selectedListId) { mutableStateOf(true) }
    var habits by rememberSaveable(state.selectedListId) { mutableStateOf(false) }
    var calendarText by rememberSaveable { mutableStateOf(state.today.toString()) }
    var calendarDay by rememberSaveable { mutableLongStateOf(state.today.toEpochDay()) }
    var calendarError by remember { mutableStateOf(false) }
    val listItems = state.snapshot.activeOccurrences.filter { it.fields.listId == state.selectedListId }
    val visible = when (page) {
        TaskPage.LIST -> if (habits) emptyList() else listItems
        TaskPage.TODAY -> if (filter == 2) emptyList() else state.snapshot.activeOccurrences.filter {
            it.inDay(state.today) }
        TaskPage.REVIEW -> if (filter == 2) emptyList() else state.snapshot.activeOccurrences.filter {
            it.needsReview(state.today) }
        TaskPage.CALENDAR -> state.snapshot.activeOccurrences.filter { it.inDay(LocalDate.ofEpochDay(calendarDay)) }
    }.sortedWith(compareBy<TaskOccurrence> { it.position }.thenBy { it.id })
    val pending = visible.filter { it.status == TaskStatus.OPEN }
    val resolved = visible.filter { it.status != TaskStatus.OPEN }
    val dayItems = if (page == TaskPage.TODAY) pending.filter { it.fields.deadline == DeadlineKind.DAY } else pending
    val weeklyItems = if (page == TaskPage.TODAY) pending.filter { it.fields.deadline != DeadlineKind.DAY }
        else emptyList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { header() }
        item { TaskError(state, onRetry, onDismissError) }
        if (state.loading) item { Text(stringResource(R.string.loading_tasks)) }
        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (page == TaskPage.LIST) {
            item {
                Text(state.snapshot.activeLists.firstOrNull { it.id == state.selectedListId }?.name
                    ?: stringResource(R.string.inbox), style = MaterialTheme.typography.headlineSmall)
                ChoiceRow(R.string.choose_list, listOf(R.string.tasks, R.string.habits), if (habits) 1 else 0) {
                    habits = it == 1 }
                TextButton(onManage) { Text(stringResource(R.string.manage_lists)) }
            }
        }
        if (page == TaskPage.CALENDAR) item {
            Text(stringResource(R.string.calendar_task_date))
            Input(R.string.date_format, calendarText) { calendarText = it }
            TextButton({
                val date = runCatching { LocalDate.parse(calendarText) }.getOrNull()
                calendarError = date == null
                if (date != null) calendarDay = date.toEpochDay()
            }) { Text(stringResource(R.string.show_date)) }
            if (calendarError) Text(stringResource(R.string.task_invalid_input))
        }
        if (page != TaskPage.REVIEW && !habits && filter != 2) item {
            Button(onNew, enabled = !state.loading && !state.busy) { Text(stringResource(R.string.new_task)) }
        }
        if (!state.loading && visible.isEmpty() && !(extraItems != null &&
                (habits || filter == 2 || page == TaskPage.REVIEW || page == TaskPage.TODAY))) item {
            Text(stringResource(if ((page == TaskPage.LIST && habits) || filter == 2)
                R.string.habits_empty_detail else if (page == TaskPage.REVIEW)
                R.string.review_empty_detail else R.string.task_empty_real))
        }
        items(dayItems, key = { it.id }) { o ->
            TaskRow(o, state.busy, page == TaskPage.LIST, { onOpen(o.id) }, onAction)
        }
        if (weeklyItems.isNotEmpty()) {
            item { Text(stringResource(R.string.weekly_section), style = MaterialTheme.typography.titleMedium) }
            items(weeklyItems, key = { it.id }) { o -> TaskRow(o, state.busy, false, { onOpen(o.id) }, onAction) }
        }
        if (resolved.isNotEmpty()) {
            item { TextButton({ collapsed = !collapsed }) {
                Text(stringResource(R.string.resolved_section, resolved.size)) } }
            if (!collapsed) items(resolved, key = { it.id }) { o ->
                TaskRow(o, state.busy, false, { onOpen(o.id) }, onAction)
            }
        }
        if (extraItems != null && ((page == TaskPage.LIST && habits) ||
                (page in listOf(TaskPage.TODAY, TaskPage.REVIEW) && filter != 1))) extraItems()
    }
}

@Composable
fun TaskRow(o: TaskOccurrence, busy: Boolean, reorder: Boolean, onOpen: () -> Unit, onAction: (TaskAction) -> Unit) {
    OutlinedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(o.fields.title, style = MaterialTheme.typography.titleSmall)
            val status = when (o.status) {
                TaskStatus.DONE -> R.string.task_status_done
                TaskStatus.NOT_DONE -> R.string.task_status_not_done
                else -> R.string.task_status_open
            }
            Text(stringResource(status), style = MaterialTheme.typography.labelMedium)
            Text(o.dueDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: stringResource(R.string.no_date),
                style = MaterialTheme.typography.bodySmall)
            o.fields.minute?.let { Text("%02d:%02d".format(it / 60, it % 60)) }
            if (reorder) Row {
                TextButton({ onAction(TaskAction.Reorder(o.id, -1)) }, enabled = !busy) {
                    Text(stringResource(R.string.move_up)) }
                TextButton({ onAction(TaskAction.Reorder(o.id, 1)) }, enabled = !busy) {
                    Text(stringResource(R.string.move_down)) }
            }
        }
    }
}

@Composable
fun TaskDialogs(state: TasksUiState, viewModel: TasksViewModel) {
    state.financialPreview?.let { preview ->
        com.ethos.app.core.ui.accounting.FinancialConfirmation(preview, state.busy,
            { viewModel.confirmFinancial(true) }, { viewModel.confirmFinancial(false) })
        return
    }
    val error: @Composable () -> Unit = { TaskError(state, viewModel::retry, viewModel::dismissError) }
    state.draft?.let { draft ->
        TaskEditor(draft, state.snapshot.activeLists, state.busy, error, viewModel::updateDraft,
            viewModel::difficulty, viewModel::repeat, viewModel::saveDraft, { viewModel.updateDraft(null) })
    }
    val detail = state.snapshot.activeOccurrences.firstOrNull { it.id == state.detailId }
    if (detail != null && state.draft == null && state.deleteTaskId == null) {
        TaskDetail(detail, state, error, { viewModel.detail(null) }, { viewModel.edit(detail) },
            { viewModel.complete(detail, it) }, viewModel::completionDate,
            { viewModel.action(it) }, { viewModel.requestDeleteTask(detail.id) })
    }
    if (state.uncheckedConfirmation != null) AlertDialog(
        onDismissRequest = { viewModel.confirmCompletion(false) },
        text = { Text(pluralStringResource(R.plurals.unchecked_confirm,
            state.uncheckedConfirmation, state.uncheckedConfirmation)) },
        confirmButton = { TextButton({ viewModel.confirmCompletion(true) }, enabled = !state.busy) {
            Text(stringResource(R.string.confirm_complete)) } },
        dismissButton = { TextButton({ viewModel.confirmCompletion(false) }) {
            Text(stringResource(R.string.cancel)) } })
    if (state.manageLists && state.listDraft == null && state.deleteListId == null) {
        ListManager(state, error, viewModel::newList, viewModel::listDraft, viewModel::requestDeleteList,
            viewModel::action, { viewModel.manageLists(false) })
    }
    state.listDraft?.let { list -> ListEditor(list, state.busy, error, viewModel::listDraft,
        viewModel::saveList, { viewModel.listDraft(null) }) }
    state.deleteListId?.let { id -> AlertDialog(onDismissRequest = { viewModel.requestDeleteList(null) },
        text = { Column { error(); Text(stringResource(R.string.delete_list_confirm)) } },
        confirmButton = { TextButton({ viewModel.action(TaskAction.DeleteList(id)) }, enabled = !state.busy) {
            Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton({ viewModel.requestDeleteList(null) }) {
            Text(stringResource(R.string.cancel)) } }) }
    state.deleteTaskId?.let { id ->
        val occurrence = state.snapshot.activeOccurrences.firstOrNull { it.id == id }
        if (occurrence != null) {
            val recurring = state.snapshot.definitions.first { it.id == occurrence.taskId }.rule.kind != RepeatKind.NONE
            DeleteTaskDialog(id, recurring, state.busy, error, viewModel::action, { viewModel.requestDeleteTask(null) })
        }
    }
}

@Composable
private fun TaskDetail(
    o: TaskOccurrence, state: TasksUiState, error: @Composable () -> Unit, close: () -> Unit,
    edit: () -> Unit, resolve: (Boolean) -> Unit, date: (String) -> Unit, action: (TaskAction) -> Unit,
    delete: () -> Unit,
) {
    AlertDialog(onDismissRequest = { if (!state.busy) close() }, title = { Text(o.fields.title) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            error()
            if (o.fields.description.isNotEmpty()) Text(o.fields.description)
            Text(o.dueDate?.toString() ?: stringResource(R.string.no_date))
            o.checks.forEachIndexed { i, c -> Row {
                Checkbox(c.checked, { action(TaskAction.Check(o.id, i, it)) },
                    enabled = !state.busy && o.status == TaskStatus.OPEN,
                    modifier = Modifier.semantics { contentDescription = c.label })
                Text(c.label, Modifier.padding(top = 12.dp))
            } }
            if (o.status == TaskStatus.OPEN) {
                val reminders = LocalOpenReminders.current
                TextButton({ reminders(ReminderKind.TASK, o.id) }, enabled = !state.busy) {
                    Text(stringResource(R.string.reminders)) }
                Input(R.string.actual_completion, state.completionText, !state.busy, change = date)
                Button({ resolve(true) }, enabled = !state.busy) { Text(stringResource(R.string.complete_task)) }
                TextButton({ resolve(false) }, enabled = !state.busy) { Text(stringResource(R.string.not_done)) }
                TextButton(edit, enabled = !state.busy) { Text(stringResource(R.string.edit)) }
                ListChoices(state.snapshot.activeLists, o.fields.listId, !state.busy) {
                    action(TaskAction.Move(o.id, it)) }
            } else TextButton({ action(TaskAction.Undo(o.id)) }, enabled = !state.busy) {
                Text(stringResource(R.string.undo_outcome)) }
            TextButton(delete, enabled = !state.busy) { Text(stringResource(R.string.delete)) }
            Text(stringResource(R.string.task_history), style = MaterialTheme.typography.titleSmall)
            val auditLabels = listOf(R.string.audit_created, R.string.audit_completed, R.string.audit_not_done,
                R.string.audit_undone, R.string.audit_missed, R.string.audit_edited, R.string.audit_deleted,
                R.string.audit_checked)
            state.snapshot.history.filter { it.occurrenceId == o.id }.asReversed().forEach { record ->
                val time = Instant.ofEpochMilli(record.recordedAt).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")))
                Text(stringResource(R.string.audit_line, stringResource(auditLabels[record.kind.ordinal]), time))
                record.actualDate?.let { Text(stringResource(R.string.actual_date_line, it.toString())) }
                if (record.kind == AuditKind.MISSED_DEADLINE) Text(stringResource(R.string.original_deadline_line,
                    record.originalDeadline.toString()))
            }
        }
    }, confirmButton = { TextButton(close, enabled = !state.busy) { Text(stringResource(R.string.close)) } })
}

@Composable
private fun ListManager(
    state: TasksUiState, error: @Composable () -> Unit, new: () -> Unit, edit: (TaskList) -> Unit,
    delete: (String) -> Unit, action: (TaskAction) -> Unit, close: () -> Unit,
) {
    AlertDialog(onDismissRequest = close, title = { Text(stringResource(R.string.manage_lists)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            error()
            state.snapshot.activeLists.forEach { list ->
                Text(list.name, style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton({ edit(list) }, enabled = !state.busy) { Text(stringResource(R.string.edit)) }
                    TextButton({ delete(list.id) }, enabled = !state.busy) { Text(stringResource(R.string.delete)) }
                }
                TextButton({ action(TaskAction.MoveList(list.id, -1)) }, enabled = !state.busy) {
                    Text(stringResource(R.string.move_up)) }
                TextButton({ action(TaskAction.MoveList(list.id, 1)) }, enabled = !state.busy) {
                    Text(stringResource(R.string.move_down)) }
                HorizontalDivider()
            }
            TextButton(new, enabled = !state.busy) { Text(stringResource(R.string.new_list)) }
        }
    }, confirmButton = { TextButton(close) { Text(stringResource(R.string.close)) } })
}

@Composable
private fun ListEditor(
    list: TaskList, busy: Boolean, error: @Composable () -> Unit, change: (TaskList) -> Unit,
    save: () -> Unit, close: () -> Unit,
) {
    AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text(stringResource(R.string.list_name)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            error()
            Input(R.string.list_name, list.name, !busy) { change(list.copy(name = it)) }
            ChoiceRow(R.string.color_label, colorLabels, list.color, !busy) { change(list.copy(color = it)) }
            ChoiceRow(R.string.icon_label, iconLabels, list.icon, !busy) { change(list.copy(icon = it)) }
        } }, confirmButton = { TextButton(save, enabled = !busy) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(close, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun DeleteTaskDialog(
    id: String, recurring: Boolean, busy: Boolean, error: @Composable () -> Unit,
    action: (TaskAction) -> Unit, close: () -> Unit,
) {
    var scope by rememberSaveable(id) { mutableStateOf(ChangeScope.ONLY_THIS) }
    AlertDialog(onDismissRequest = { if (!busy) close() }, text = { Column {
        error()
        Text(stringResource(R.string.delete_task_confirm))
        if (recurring) ScopeChoice(scope, !busy) { scope = it }
    } }, confirmButton = { TextButton({ action(TaskAction.Delete(id, scope)) }, enabled = !busy) {
        Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(close, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
}

@Preview
@Composable
private fun TaskRowPreview() {
    MaterialTheme {
        TaskRow(TaskOccurrence("preview:1", "preview", 1, TaskFields(title = "Organizar a próxima semana"),
            null, null, null), false, true, {}, {})
    }
}
