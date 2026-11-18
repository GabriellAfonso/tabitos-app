package com.ethos.app.features.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.icons.AppIconTables
import com.ethos.app.core.reminders.domain.ReminderKind
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.ui.accounting.FinancialConfirmation
import com.ethos.app.core.ui.components.CHECK_ANIMATION_MILLIS
import com.ethos.app.core.ui.components.CheckboxRow
import com.ethos.app.core.ui.components.ColorPicker
import com.ethos.app.core.ui.components.ContentFilter
import com.ethos.app.core.ui.components.IconPicker
import com.ethos.app.core.ui.components.LazyReorderState
import com.ethos.app.core.ui.components.RoundCheck
import com.ethos.app.core.ui.components.itemColors
import com.ethos.app.core.ui.components.lazyReorderGestures
import com.ethos.app.core.ui.components.moved
import com.ethos.app.core.ui.components.reorderActions
import com.ethos.app.core.ui.components.reorderableItem
import com.ethos.app.core.ui.format.EthosDates
import com.ethos.app.core.ui.format.label
import com.ethos.app.core.ui.reminders.LocalOpenReminders
import com.ethos.app.core.ui.timers.LocalOpenTimer
import com.ethos.app.core.ui.timers.TimerChip
import com.ethos.app.core.ui.timers.TimerChipButton
import java.time.LocalDate
import kotlinx.coroutines.delay

enum class TaskPage { LIST, TODAY, REVIEW }

private const val UNCONFIRMED_CHECK_RESET_MILLIS = 1_500L

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
fun TasksContent(
    state: TasksUiState, page: TaskPage, filter: ContentFilter, onOpen: (String) -> Unit,
    onAction: (TaskAction) -> Unit, onRetry: () -> Unit, onDismissError: () -> Unit,
    extraItems: (LazyListScope.() -> Unit)? = null,
    onComplete: (String) -> Unit = {},
    onNotDone: (String) -> Unit = {},
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
        start = 20.dp, top = 20.dp, end = 20.dp, bottom = if (page == TaskPage.REVIEW) 20.dp else 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { header() }
        item { TaskError(state, onRetry, onDismissError) }
        if (state.loading) item { Text(stringResource(R.string.loading_tasks)) }
        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (page == TaskPage.LIST) {
            item {
                val selectedList = state.snapshot.activeLists.firstOrNull { it.id == state.selectedListId }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    selectedList?.let {
                        Icon(painterResource(it.iconDrawable), contentDescription = null,
                            tint = itemColors[it.color])
                    }
                    Text(selectedList?.name ?: stringResource(R.string.inbox),
                        style = MaterialTheme.typography.headlineSmall)
                }
                ChoiceRow(R.string.choose_list, listOf(R.string.tasks, R.string.habits), if (habits) 1 else 0) {
                    habits = it == 1 }
            }
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
            } else Modifier.animateItem()
            TaskRow(o, state.busy, { onOpen(o.id) }, state.today, placement, o.taskId in recurring,
                timerChip(o), onNotDone = { onNotDone(o.id) }) { onComplete(o.id) }
        }
        if (sections.weekly.isNotEmpty()) {
            item { Text(stringResource(R.string.weekly_section), style = MaterialTheme.typography.titleMedium) }
            items(sections.weekly, key = { it.id }) { o ->
                TaskRow(o, state.busy, { onOpen(o.id) }, state.today, Modifier.animateItem(),
                    recurring = o.taskId in recurring, timer = timerChip(o), onNotDone = { onNotDone(o.id) }) {
                    onComplete(o.id) }
            }
        }
        if (showsHabitItems) extraItems?.invoke(this)
        if (sections.completed.isNotEmpty()) {
            item("completed-section") { TextButton({ collapsed = !collapsed }) {
                Text(stringResource(R.string.resolved_section, sections.completed.size)) } }
            if (!collapsed) items(sections.completed, key = { it.id }) { o ->
                TaskRow(o, state.busy, { onOpen(o.id) }, state.today, Modifier.animateItem(),
                    recurring = o.taskId in recurring) { onComplete(o.id) }
            }
        }
    }
}

/** Direct checkbox and independent detail target; completed cards remain readable with muted colors. */
@Composable
fun TaskRow(
    o: TaskOccurrence, busy: Boolean, onOpen: () -> Unit, today: LocalDate,
    modifier: Modifier = Modifier, recurring: Boolean = false, timer: TimerChip? = null,
    onNotDone: () -> Unit = onOpen,
    onComplete: () -> Unit = onOpen,
) {
    var showCheckMenu by remember(o.id, o.status) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    // The tick animates first and only then the completion is sent, so the row does not jump to another
    // section (or close a confirmation flow) before the user sees it.
    var completing by remember(o.id, o.status) { mutableStateOf(false) }
    LaunchedEffect(completing) {
        if (completing) {
            delay(CHECK_ANIMATION_MILLIS)
            onComplete()
            // Status stays OPEN when a confirmation is cancelled; unticking keeps the box honest.
            delay(UNCONFIRMED_CHECK_RESET_MILLIS)
            completing = false
        }
    }
    OutlinedCard(onClick = onOpen, modifier = modifier.fillMaxWidth().testTag("task:${o.id}"), enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = if (o.status != TaskStatus.OPEN)
            MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(start = 2.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            val open = o.status == TaskStatus.OPEN
            val checkLabel = stringResource(when (o.status) {
                TaskStatus.OPEN -> R.string.task_quick_complete
                TaskStatus.DONE -> R.string.task_quick_uncomplete
                else -> R.string.task_resolved_open
            }, o.fields.title)
            Box {
                Box(Modifier.size(48.dp).combinedClickable(
                    enabled = !busy && !completing,
                    role = Role.Checkbox,
                    interactionSource = null,
                    indication = null,
                    onLongClickLabel = stringResource(R.string.task_status_options),
                    onLongClick = if (open) ({ showCheckMenu = true }) else null,
                    onClick = {
                        when (o.status) {
                            TaskStatus.NOT_DONE -> onOpen()
                            TaskStatus.OPEN -> {
                                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                completing = true
                            }
                            else -> onComplete()
                        }
                    },
                ).semantics {
                    contentDescription = checkLabel
                    toggleableState = ToggleableState(o.status == TaskStatus.DONE)
                }, contentAlignment = Alignment.Center) {
                    RoundCheck(checked = o.status == TaskStatus.DONE || completing, crossed = o.status == TaskStatus.NOT_DONE)
                }
                DropdownMenu(expanded = showCheckMenu && !busy, onDismissRequest = { showCheckMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.task_mark_done)) }, onClick = {
                        showCheckMenu = false
                        onComplete()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.task_mark_not_done)) }, onClick = {
                        showCheckMenu = false
                        onNotDone()
                    })
                }
            }
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
                // Recurrence sits with the date, leaving the trailing edge to the timer control alone.
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (recurring) Icon(painterResource(R.drawable.ic_repeat),
                        contentDescription = stringResource(R.string.task_repeats),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (timer != null && o.status == TaskStatus.OPEN) TimerChipButton(timer, o.fields.title, !busy)
        }
    }
}

/** [openHistory] receives the task series id; without it the popup has no "Ver histórico" link. */
@Composable
fun TaskDialogs(state: TasksUiState, viewModel: TasksViewModel, openHistory: ((String) -> Unit)? = null) {
    state.financialPreview?.let { preview ->
        FinancialConfirmation(preview, state.busy,
            { viewModel.confirmFinancial(true) }, { viewModel.confirmFinancial(false) })
        return
    }
    val error: @Composable () -> Unit = { TaskError(state, viewModel::retry, viewModel::dismissError) }
    state.draft?.let { draft ->
        TaskEditor(draft, state.snapshot.activeLists,
            state.snapshot.activeOccurrences.firstOrNull { it.id == draft.id }.takeIf { draft.editing },
            state.today, state.busy, error, viewModel::updateDraft,
            onDifficulty = if (state.gameMode) viewModel::difficulty else null, onRepeat = viewModel::repeatRule,
            onSave = viewModel::saveDraft, onClose = viewModel::closeDraft, onNewList = viewModel::newListForDraft)
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
        val openReminders = LocalOpenReminders.current
        val openTimer = LocalOpenTimer.current
        TaskDetail(taskDetailModel(state.snapshot, detail, state.today, state.reminders, state.durations),
            state.snapshot.activeLists, state.today, state.busy, error, TaskDetailActions(
                close = { viewModel.detail(null) },
                complete = { viewModel.complete(detail, true) },
                completeOn = { viewModel.completeOn(detail, it) },
                notDone = { viewModel.complete(detail, false) },
                undoResult = { viewModel.action(TaskAction.Undo(detail.id)) },
                edit = { viewModel.edit(detail) },
                delete = { viewModel.requestDeleteTask(detail.id) },
                move = { viewModel.action(TaskAction.Move(detail.id, it)) },
                check = { index, checked -> viewModel.action(TaskAction.Check(detail.id, index, checked)) },
                openReminders = { openReminders(ReminderKind.TASK, detail.id) },
                openTimer = { openTimer(TimerKind.TASK, detail.id) },
                postpone = { viewModel.postpone(detail, it) },
                history = openHistory?.let { open -> { open(detail.taskId) } },
            ))
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
private fun ListEditor(
    list: TaskList, busy: Boolean, error: @Composable () -> Unit, change: (TaskList) -> Unit,
    save: () -> Unit, close: () -> Unit,
) {
    AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text(stringResource(R.string.list_name)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            error()
            Input(R.string.list_name, list.name, !busy) { change(list.copy(name = it)) }
            ColorPicker(list.color, !busy) { change(list.copy(color = it)) }
            IconPicker(R.string.icon_label, listPrincipalIcons, AppIconTables.list[list.icon], !busy,
                tint = itemColors[list.color]) { change(list.copy(icon = AppIconTables.list.indexOf(it))) }
        } }, confirmButton = { TextButton(save, enabled = !busy) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(close, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun DeleteTaskDialog(
    id: String, recurring: Boolean, busy: Boolean, error: @Composable () -> Unit,
    action: (TaskAction) -> Unit, close: () -> Unit,
) {
    var scope by rememberSaveable(id) { mutableStateOf(ChangeScope.ONLY_THIS) }
    var eraseHistory by rememberSaveable(id) { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) close() }, text = { Column {
        error()
        Text(stringResource(R.string.delete_task_confirm))
        if (recurring) ScopeChoice(scope, !busy) { scope = it }
        CheckboxRow(R.string.delete_task_erase_history, eraseHistory, !busy) { eraseHistory = it }
    } }, confirmButton = { TextButton({ action(TaskAction.Delete(id, scope, eraseHistory)) }, enabled = !busy) {
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
