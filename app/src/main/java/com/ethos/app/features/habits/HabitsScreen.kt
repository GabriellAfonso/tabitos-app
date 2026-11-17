package com.ethos.app.features.habits

import com.ethos.app.core.ui.accounting.FinancialConfirmation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.habits.domain.Habit
import com.ethos.app.core.habits.domain.HabitStatus
import com.ethos.app.core.tasks.domain.TaskList
import com.ethos.app.core.ui.components.CheckboxRow
import com.ethos.app.core.ui.reminders.LocalOpenReminders
import com.ethos.app.core.ui.timers.LocalOpenTimer
import com.ethos.app.core.ui.trackers.LocalOpenTracker
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.reminders.domain.ReminderKind
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
fun HabitsScreen(viewModel: HabitsViewModel, content: @Composable (HabitsUiState) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HabitsRefreshEffect(viewModel)
    content(state)
}

/** Keeps periods current while visible; a day change also settles closed periods (spec 014). */
@Composable
fun HabitsRefreshEffect(viewModel: HabitsViewModel) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { viewModel.refresh(); delay(60_000) }
        }
    }
}

/** [openHistory] receives the habit id; without it the popup has no "Ver histórico" link. */
@Composable
fun HabitDialogs(
    state: HabitsUiState, lists: List<TaskList>, vm: HabitsViewModel, openHistory: ((String) -> Unit)? = null,
) {
    state.financialPreview?.let { preview ->
        FinancialConfirmation(preview, state.busy,
            { vm.confirmFinancial(true) }, { vm.confirmFinancial(false) })
        return
    }
    if (state.draft != null) HabitEditor(state, lists,
        HabitEditorActions(vm::draft, vm::difficulty, vm::save, vm::retry))
    val h = state.snapshot.habits.firstOrNull { it.id == state.detail }
    state.eraseConfirmation?.let { id ->
        AlertDialog(onDismissRequest = { if (!state.busy) vm.requestErase(null) },
            title = { Text(stringResource(R.string.habit_erase_completely)) },
            text = { Column { HabitError(state, vm::retry); Text(stringResource(R.string.habit_erase_confirm)) } },
            confirmButton = { TextButton({ vm.erase(id) }, enabled = !state.busy) {
                Text(stringResource(R.string.habit_erase_completely), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ vm.requestErase(null) }, enabled = !state.busy) {
                Text(stringResource(R.string.cancel)) } })
        return
    }
    if (h != null && state.draft == null && state.lifecycle == null) HabitDetailDialog(state, h, lists, vm, openHistory)
    state.lifecycle?.let { d ->
        AlertDialog(onDismissRequest = { if (!state.busy) vm.lifecycle(null) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HabitError(state, vm::retry)
                Text(stringResource(R.string.habit_lifecycle_hint))
                if (d.status == HabitStatus.ACTIVE) HabitLists(lists, d.listId, !state.busy) {
                    vm.lifecycle(d.copy(listId = it, destinationChosen = true)) }
                if (d.status == HabitStatus.EXCLUDED) {
                    CheckboxRow(R.string.delete_habit_erase_history, d.eraseHistory, !state.busy) {
                        vm.lifecycle(d.copy(eraseHistory = it)) }
                }
            }
        }, confirmButton = { TextButton(vm::confirmLifecycle, enabled = !state.busy) {
            Text(stringResource(R.string.habit_confirm_change)) } },
            dismissButton = { TextButton({ vm.lifecycle(null) }, enabled = !state.busy) {
                Text(stringResource(R.string.cancel)) } })
    }
    // Last, so it is drawn above the habit popup that asked for it.
    if (state.timedConfirmation != null) AlertDialog(
        onDismissRequest = { vm.confirmTimed(false) },
        title = { Text(stringResource(R.string.timed_confirm_title)) },
        text = { Text(stringResource(R.string.timed_confirm_body)) },
        confirmButton = { TextButton({ vm.confirmTimed(true) }, enabled = !state.busy) {
            Text(stringResource(R.string.timed_confirm_action)) } },
        dismissButton = { TextButton({ vm.confirmTimed(false) }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun HabitDetailDialog(
    state: HabitsUiState, h: Habit, lists: List<TaskList>, vm: HabitsViewModel, openHistory: ((String) -> Unit)?,
) {
    val model = habitDetailModel(state, h.id) ?: return
    val timer = LocalOpenTimer.current
    val tracker = LocalOpenTracker.current
    val reminders = LocalOpenReminders.current
    fun lifecycle(status: HabitStatus) = vm.lifecycle(HabitLifecycleDraft(h.id, status, listId = h.listId))
    HabitDetail(model, lists.firstOrNull { it.id == h.listId }, state.today, state.busy,
        error = { HabitError(state, vm::retry) },
        actions = HabitDetailActions(
            close = { vm.detail(null) },
            setPractice = { date, done -> vm.setPractice(h.id, date, done) },
            editDay = vm::editDay,
            addOccurrence = { vm.addOccurrence(h.id, it) },
            removeOccurrence = { vm.removeOccurrence(h.id, it) },
            openTimer = { timer(TimerKind.HABIT, h.id) },
            edit = { vm.edit(h) },
            reminders = { reminders(ReminderKind.HABIT, h.id) },
            tracker = { tracker(h.id) },
            pause = { lifecycle(HabitStatus.PAUSED) },
            resume = { lifecycle(HabitStatus.ACTIVE) },
            exclude = { lifecycle(HabitStatus.EXCLUDED) },
            restore = {
                // The habit's list may have been deleted meanwhile; then the destination must be chosen again.
                val listExists = lists.any { it.id == h.listId }
                vm.lifecycle(HabitLifecycleDraft(h.id, HabitStatus.ACTIVE, listId = h.listId.takeIf { listExists },
                    requiresDestination = h.listId != null && !listExists))
            },
            erase = { vm.requestErase(h.id) },
            history = openHistory?.let { open -> { open(h.id) } },
        ))
    state.dayEditor?.let { date ->
        HabitDaySheet(model, date, state.today, state.busy, add = { vm.addOccurrence(h.id, date) },
            remove = { vm.removeOccurrence(h.id, date) }, dismiss = { vm.editDay(null) })
    }
}
