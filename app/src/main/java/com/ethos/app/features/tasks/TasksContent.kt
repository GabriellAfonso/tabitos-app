package com.ethos.app.features.tasks

import com.ethos.app.core.ui.accounting.FinancialConfirmation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import com.ethos.app.core.ui.components.ContentFilter
import com.ethos.app.core.ui.timers.TimerChip
import com.ethos.app.core.ui.timers.TimerChipButton
import com.ethos.app.core.ui.components.DragHandle
import com.ethos.app.core.ui.components.LazyReorderState
import com.ethos.app.core.ui.components.ReorderableColumn
import com.ethos.app.core.ui.components.lazyReorderGestures
import com.ethos.app.core.ui.components.moved
import com.ethos.app.core.ui.components.reorderActions
import com.ethos.app.core.ui.components.reorderableItem
import com.ethos.app.core.ui.components.RoundCheck
import com.ethos.app.core.ui.format.EthosDates
import com.ethos.app.core.ui.format.label
import com.ethos.app.core.ui.components.ColorPicker
import com.ethos.app.core.ui.components.itemColors
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.ui.reminders.LocalOpenReminders
import com.ethos.app.core.ui.timers.LocalOpenTimer
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.reminders.domain.ReminderKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class TaskPage { LIST, TODAY, REVIEW }
private val listSymbols = listOf("☰", "▣", "⌂", "▤", "♡", "☰")

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

/**
 * Lists section of the drawer. Items use the same full-width drawer rows as the destinations above, so the
 * whole row is the touch target. [highlighted] marks the open list while the tasks page is showing.
 */
@Composable
fun TaskDrawer(
    state: TasksUiState, highlighted: Boolean, onSelect: (String?) -> Unit, onNew: () -> Unit, onManage: () -> Unit,
) {
    fun ifIdle(action: () -> Unit) { if (!state.busy) action() }
    NavigationDrawerItem(label = { Text(stringResource(R.string.inbox)) },
        selected = highlighted && state.selectedListId == null, onClick = { ifIdle { onSelect(null) } })
    state.snapshot.activeLists.forEach { list ->
        NavigationDrawerItem(
            label = { Text(list.name) },
            icon = { Text(listSymbols[list.icon], color = itemColors[list.color]) },
            selected = highlighted && state.selectedListId == list.id,
            onClick = { ifIdle { onSelect(list.id) } },
        )
    }
    NavigationDrawerItem(label = { Text(stringResource(R.string.new_list)) }, icon = { Text("+") },
        selected = false, onClick = { ifIdle(onNew) })
    NavigationDrawerItem(label = { Text(stringResource(R.string.manage_lists)) }, icon = { Text("✎") },
        selected = false, onClick = { ifIdle(onManage) })
}

@Composable
fun TasksContent(
    state: TasksUiState, page: TaskPage, filter: ContentFilter, onNew: () -> Unit, onOpen: (String) -> Unit,
    onAction: (TaskAction) -> Unit, onManage: () -> Unit, onRetry: () -> Unit, onDismissError: () -> Unit,
    extraItems: (LazyListScope.() -> Unit)? = null,
    onComplete: (String) -> Unit = {},
    timerChip: (TaskOccurrence) -> TimerChip? = { null },
    header: @Composable () -> Unit = {},
) {
    val recurring = state.snapshot.definitions.filter { it.rule.kind != RepeatKind.NONE }.map { it.id }.toSet()
    var collapsed by rememberSaveable(page, state.selectedListId) { mutableStateOf(false) }
    var habits by rememberSaveable(state.selectedListId) { mutableStateOf(false) }
    val sections = remember(state.snapshot, page, filter, state.today, state.selectedListId, habits) {
        state.snapshot.sections(page, filter, state.today, state.selectedListId, habits)
    }
    val pending = sections.pending
    val showsHabitItems = extraItems != null && sections.showsHabits
    val listState = rememberLazyListState()
    val ids = pending.map { it.id }
    val drag = remember(page, state.selectedListId, ids) { LazyReorderState(listState, ids) }
    val dragEnabled = page == TaskPage.LIST && !state.busy && !state.loading && !state.storageError
    val edge = with(LocalDensity.current) { 64.dp.toPx() }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(drag, drag.draggedId, dragEnabled) {
        if (dragEnabled) drag.autoScroll(edge) else if (drag.draggedId != null) drag.cancel()
    }
    LaunchedEffect(drag, state.storageError, state.error) {
        if (state.storageError || state.error != null) drag.cancel()
    }
    val pendingById = pending.associateBy { it.id }
    val dayItems = if (page == TaskPage.LIST) drag.order.mapNotNull(pendingById::get) else pending
    LazyColumn(Modifier.fillMaxSize().lazyReorderGestures(drag, dragEnabled,
        { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }, { onAction(TaskAction.Order(it)) }),
        state = listState, contentPadding = PaddingValues(
        start = 20.dp, top = 20.dp, end = 20.dp, bottom = if (page == TaskPage.TODAY) 96.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { header() }
        item { TaskError(state, onRetry, onDismissError) }
        if (state.loading) item { Text(stringResource(R.string.loading_tasks)) }
        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (page == TaskPage.LIST) {
            item {
                val selectedList = state.snapshot.activeLists.firstOrNull { it.id == state.selectedListId }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    selectedList?.let { Text(listSymbols[it.icon], color = itemColors[it.color]) }
                    Text(selectedList?.name ?: stringResource(R.string.inbox),
                        style = MaterialTheme.typography.headlineSmall)
                }
                ChoiceRow(R.string.choose_list, listOf(R.string.tasks, R.string.habits), if (habits) 1 else 0) {
                    habits = it == 1 }
                TextButton(onManage) { Text(stringResource(R.string.manage_lists)) }
            }
        }
        if (page == TaskPage.LIST && sections.showsTasks) item {
            Button(onNew, enabled = !state.loading && !state.busy) { Text(stringResource(R.string.new_task)) }
        }
        if (!state.loading && sections.isEmpty && !showsHabitItems) item {
            Text(stringResource(when {
                !sections.showsTasks -> R.string.habits_empty_detail
                page == TaskPage.REVIEW -> R.string.review_empty_detail
                else -> R.string.task_empty_real
            }))
        }
        items(dayItems, key = { it.id }) { o ->
            val placement = if (page == TaskPage.LIST) {
                val index = ids.indexOf(o.id)
                Modifier.reorderableItem(this, drag, o.id).reorderActions(index, ids.lastIndex, dragEnabled) { to ->
                    onAction(TaskAction.Order(ids.moved(index, to)))
                }
            } else Modifier
            TaskRow(o, state.busy, { onOpen(o.id) }, state.today, placement, o.taskId in recurring,
                timerChip(o)) { onComplete(o.id) }
        }
        if (sections.weekly.isNotEmpty()) {
            item { Text(stringResource(R.string.weekly_section), style = MaterialTheme.typography.titleMedium) }
            items(sections.weekly, key = { it.id }) { o ->
                TaskRow(o, state.busy, { onOpen(o.id) }, state.today, recurring = o.taskId in recurring,
                    timer = timerChip(o)) {
                    onComplete(o.id) }
            }
        }
        if (sections.notDone.isNotEmpty()) {
            item { Text(stringResource(R.string.not_done_section, sections.notDone.size)) }
            items(sections.notDone, key = { it.id }) { o ->
                TaskRow(o, state.busy, { onOpen(o.id) }, state.today, recurring = o.taskId in recurring)
            }
        }
        if (showsHabitItems) extraItems?.invoke(this)
        if (sections.completed.isNotEmpty()) {
            item("completed-section") { TextButton({ collapsed = !collapsed }) {
                Text(stringResource(R.string.resolved_section, sections.completed.size)) } }
            if (!collapsed) items(sections.completed, key = { it.id }) { o ->
                TaskRow(o, state.busy, { onOpen(o.id) }, state.today, recurring = o.taskId in recurring) {
                    onComplete(o.id) }
            }
        }
    }
}

/** Direct checkbox and independent detail target; completed cards remain readable with muted colors. */
@Composable
fun TaskRow(
    o: TaskOccurrence, busy: Boolean, onOpen: () -> Unit, today: LocalDate,
    modifier: Modifier = Modifier, recurring: Boolean = false, timer: TimerChip? = null,
    onComplete: () -> Unit = onOpen,
) {
    OutlinedCard(onClick = onOpen, modifier = modifier.fillMaxWidth().testTag("task:${o.id}"), enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = if (o.status == TaskStatus.DONE)
            MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(start = 2.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            val open = o.status == TaskStatus.OPEN
            val checkLabel = stringResource(when (o.status) {
                TaskStatus.OPEN -> R.string.task_quick_complete
                TaskStatus.DONE -> R.string.task_quick_uncomplete
                else -> R.string.task_resolved_open
            }, o.fields.title)
            Box(Modifier.size(48.dp).toggleable(o.status == TaskStatus.DONE, enabled = !busy, role = Role.Checkbox,
                onValueChange = { if (o.status == TaskStatus.NOT_DONE) onOpen() else onComplete() })
                .semantics { contentDescription = checkLabel },
                contentAlignment = Alignment.Center) { RoundCheck(o.status == TaskStatus.DONE) }
            Column(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(o.fields.title, style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (open) null else TextDecoration.LineThrough,
                    color = if (open) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                val status = when (o.status) {
                    TaskStatus.DONE -> R.string.task_status_done
                    TaskStatus.NOT_DONE -> R.string.task_status_not_done
                    else -> R.string.task_status_open
                }
                val dateLabel = o.dueDate?.let { EthosDates.short(it, today).label() }
                    ?: stringResource(R.string.no_date)
                val parts = listOfNotNull(o.fields.minute?.let(EthosDates::time),
                    dateLabel,
                    stringResource(status))
                Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (timer != null && o.status == TaskStatus.OPEN) TimerChipButton(timer, o.fields.title, !busy)
            if (recurring) Icon(painterResource(R.drawable.ic_task_repeat),
                contentDescription = stringResource(R.string.task_repeats),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f))
        }
    }
}

@Composable
fun TaskDialogs(state: TasksUiState, viewModel: TasksViewModel) {
    state.financialPreview?.let { preview ->
        FinancialConfirmation(preview, state.busy,
            { viewModel.confirmFinancial(true) }, { viewModel.confirmFinancial(false) })
        return
    }
    val error: @Composable () -> Unit = { TaskError(state, viewModel::retry, viewModel::dismissError) }
    state.draft?.let { draft ->
        TaskEditor(draft, state.snapshot.activeLists,
            state.snapshot.activeOccurrences.firstOrNull { it.id == draft.id }.takeIf { draft.editing },
            state.today, state.busy, error, viewModel::updateDraft, viewModel::difficulty, viewModel::repeatRule,
            viewModel::saveDraft, viewModel::closeDraft, viewModel::newListForDraft)
        if (state.discardConfirmation) AlertDialog(onDismissRequest = { viewModel.confirmDiscard(false) },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = { TextButton({ viewModel.confirmDiscard(true) }) {
                Text(stringResource(R.string.discard)) } },
            dismissButton = { TextButton({ viewModel.confirmDiscard(false) }) {
                Text(stringResource(R.string.keep_editing)) } })
    }
    val detail = state.snapshot.activeOccurrences.firstOrNull { it.id == state.detailId }
    if (detail != null && state.draft == null && state.deleteTaskId == null) {
        TaskDetail(detail, state, error, { viewModel.detail(null) }, { viewModel.edit(detail) },
            { viewModel.complete(detail, it) }, viewModel::completionDate,
            { viewModel.action(it) }, { viewModel.requestDeleteTask(detail.id) })
    }
    if (state.timedConfirmation != null) AlertDialog(
        onDismissRequest = { viewModel.confirmTimed(false) },
        title = { Text(stringResource(R.string.timed_confirm_title)) },
        text = { Text(stringResource(R.string.timed_confirm_body)) },
        confirmButton = { TextButton({ viewModel.confirmTimed(true) }, enabled = !state.busy) {
            Text(stringResource(R.string.timed_confirm_action)) } },
        dismissButton = { TextButton({ viewModel.confirmTimed(false) }) { Text(stringResource(R.string.cancel)) } })
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
            Text(listOfNotNull(o.dueDate?.let { EthosDates.long(it, state.today).label() }
                ?: stringResource(R.string.no_date), o.fields.minute?.let(EthosDates::time)).joinToString(" · "))
            o.checks.forEachIndexed { i, c -> Row {
                Checkbox(c.checked, { action(TaskAction.Check(o.id, i, it)) },
                    enabled = !state.busy && o.status == TaskStatus.OPEN,
                    modifier = Modifier.semantics { contentDescription = c.label })
                Text(c.label, Modifier.padding(top = 12.dp))
            } }
            // Only timed tasks have a timer; a free stopwatch is not part of tasks (spec 016).
            if (state.isTimed(o)) {
                val timer = LocalOpenTimer.current
                TextButton({ timer(TimerKind.TASK, o.id) }, enabled = !state.busy) {
                    Text(stringResource(R.string.timer_button)) }
            }
            if (o.status == TaskStatus.OPEN) {
                val reminders = LocalOpenReminders.current
                TextButton({ reminders(ReminderKind.TASK, o.id) }, enabled = !state.busy) {
                    Text(stringResource(R.string.reminders)) }
                DateField(R.string.actual_completion, state.completionText, state.today, !state.busy, date,
                    max = state.today)
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
                val moment = Instant.ofEpochMilli(record.recordedAt).atZone(ZoneId.systemDefault())
                val time = EthosDates.shortPlain(moment.toLocalDate(), state.today) + " " +
                    EthosDates.time(moment.hour * 60 + moment.minute)
                Text(stringResource(R.string.audit_line, stringResource(auditLabels[record.kind.ordinal]), time))
                record.actualDate?.let { Text(stringResource(R.string.actual_date_line,
                    EthosDates.shortPlain(it, state.today))) }
                if (record.kind == AuditKind.MISSED_DEADLINE) Text(stringResource(R.string.original_deadline_line,
                    record.originalDeadline?.let { EthosDates.shortPlain(it, state.today) }.orEmpty()))
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
            ReorderableColumn(state.snapshot.activeLists, { it.id }, !state.busy,
                onReorder = { lists -> action(TaskAction.OrderLists(lists.map { it.id })) }) { list, handle ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DragHandle(handle)
                        Text(listSymbols[list.icon], color = itemColors[list.color],
                            modifier = Modifier.padding(end = 10.dp))
                        Text(list.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    }
                    Row {
                        TextButton({ edit(list) }, enabled = !state.busy) { Text(stringResource(R.string.edit)) }
                        TextButton({ delete(list.id) }, enabled = !state.busy) { Text(stringResource(R.string.delete)) }
                    }
                    HorizontalDivider()
                }
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
            ColorPicker(list.color, !busy) { change(list.copy(color = it)) }
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
            null, null, null), false, {}, LocalDate.of(2026, 11, 11))
    }
}

/** "Concluída pelo cronômetro · Desfazer" after a timed task completes by itself (spec 016). */
@Composable
fun TimerDoneFeedback(undo: () -> Unit, dismiss: () -> Unit) {
    Snackbar(Modifier.padding(12.dp),
        action = { TextButton(undo) { Text(stringResource(R.string.undo)) } },
        dismissAction = { TextButton(dismiss) { Text(stringResource(R.string.close)) } },
    ) { Text(stringResource(R.string.task_timer_done)) }
}
