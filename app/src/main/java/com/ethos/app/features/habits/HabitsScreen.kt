package com.ethos.app.features.habits

import com.ethos.app.core.ui.accounting.FinancialConfirmation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.tasks.domain.TaskList
import com.ethos.app.core.ui.reminders.LocalOpenReminders
import com.ethos.app.core.ui.timers.LocalOpenTimer
import com.ethos.app.core.ui.trackers.LocalOpenTracker
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.reminders.domain.ReminderKind
import androidx.compose.runtime.*
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

@Composable
fun HabitDialogs(state: HabitsUiState, lists: List<TaskList>, vm: HabitsViewModel) {
    state.financialPreview?.let { preview ->
        FinancialConfirmation(preview, state.busy,
            { vm.confirmFinancial(true) }, { vm.confirmFinancial(false) })
        return
    }
    if (state.timedConfirmation != null) AlertDialog(
        onDismissRequest = { vm.confirmTimed(false) },
        title = { Text(stringResource(R.string.timed_confirm_title)) },
        text = { Text(stringResource(R.string.timed_confirm_body)) },
        confirmButton = { TextButton({ vm.confirmTimed(true) }, enabled = !state.busy) {
            Text(stringResource(R.string.timed_confirm_action)) } },
        dismissButton = { TextButton({ vm.confirmTimed(false) }) { Text(stringResource(R.string.cancel)) } })
    if (state.draft != null) HabitEditor(state, lists,
        HabitEditorActions(vm::draft, vm::difficulty, vm::save, vm::retry))
    val h = state.snapshot.habits.firstOrNull { it.id == state.detail }
    if (h != null && state.draft == null && state.lifecycle == null) {
        AlertDialog(onDismissRequest = { if (!state.busy) vm.detail(null) }, title = { Text(h.name) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HabitError(state, vm::retry)
                if (h.description.isNotBlank()) Text(h.description)
                state.streaks[h.id]?.let { streak ->
                    val unit = stringResource(when (streak.unit) {
                        HabitUnit.DAY -> R.string.habit_day
                        HabitUnit.WEEK -> R.string.habit_week
                        HabitUnit.MONTH -> R.string.habit_month
                    })
                    Text(stringResource(R.string.habit_streak, streak.current, streak.best, unit))
                    state.rates[h.id]?.let { rate ->
                        Text(stringResource(R.string.habit_consistency, streak.current, rate)) }
                    streak.historicalBest.filterKeys { it != streak.unit }.forEach { (oldUnit, best) ->
                        val oldLabel = stringResource(when (oldUnit) {
                            HabitUnit.DAY -> R.string.habit_day
                            HabitUnit.WEEK -> R.string.habit_week
                            HabitUnit.MONTH -> R.string.habit_month
                        })
                        Text(stringResource(R.string.habit_old_best, best, oldLabel))
                    }
                }
                state.periods.lastOrNull { it.habitId == h.id }?.let { HabitProgress(it, state.today) }
                Text(stringResource(R.string.habit_calendar))
                val records = state.snapshot.records.filter { it.habitId == h.id && !it.deleted }
                    .sortedWith(compareByDescending<HabitRecord> { it.date }.thenBy { it.id })
                val days = state.dayStandings[h.id].orEmpty()
                HabitCalendar(records, state.today, vm::date) { day -> days[day] }
                Text(stringResource(R.string.habit_closed_hint), style = MaterialTheme.typography.bodySmall)
                // Calendar grouped by actual date, followed by individually correctable event records.
                records.groupBy { it.date }.forEach { (day, entries) ->
                    Text(stringResource(R.string.habit_calendar_day, day.toString(), entries.size))
                }
                HabitInput(R.string.habit_actual_date, state.recordDate, !state.busy, vm::date)
                Row {
                    TextButton({ vm.date(state.today.toString()) }, enabled = !state.busy) {
                        Text(stringResource(R.string.habit_today)) }
                    TextButton({ vm.date(state.today.minusDays(1).toString()) }, enabled = !state.busy) {
                        Text(stringResource(R.string.habit_yesterday)) }
                }
                Button(vm::record, enabled = !state.busy) {
                    Text(stringResource(if (state.correctionId == null)
                        R.string.habit_record else R.string.habit_correct)) }
                records.forEach { record ->
                    Text(record.date.toString())
                    Row {
                        TextButton({ vm.correct(record) }, enabled = !state.busy) {
                            Text(stringResource(R.string.habit_correct)) }
                        TextButton({ vm.action(HabitAction.Correct(record.id, delete = true)) },
                            enabled = !state.busy) {
                            Text(stringResource(R.string.habit_delete_record)) }
                    }
                }
                Text(stringResource(R.string.habit_history_periods))
                state.periods.filter { it.habitId == h.id }.reversed().forEach { p ->
                    HabitProgress(p, state.today)
                }
                // Only timed habits have a timer; a free stopwatch is not part of habits (spec 016).
                if (h.id in state.durations) {
                    val timer = LocalOpenTimer.current
                    TextButton({ timer(TimerKind.HABIT, h.id) }, enabled = !state.busy) {
                        Text(stringResource(R.string.timer_button)) }
                }
                val tracker = LocalOpenTracker.current
                TextButton({ tracker(h.id) }, enabled = !state.busy) { Text(stringResource(R.string.tracker)) }
                if (h.status != HabitStatus.EXCLUDED) {
                    val reminders = LocalOpenReminders.current
                    TextButton({ reminders(ReminderKind.HABIT, h.id) }, enabled = !state.busy) {
                        Text(stringResource(R.string.reminders)) }
                    TextButton({ vm.edit(h) }, enabled = !state.busy) { Text(stringResource(R.string.edit_habit)) }
                    TextButton({ vm.lifecycle(HabitLifecycleDraft(h.id,
                        if (h.status == HabitStatus.ACTIVE) HabitStatus.PAUSED else HabitStatus.ACTIVE,
                        listId = h.listId)) }, enabled = !state.busy) {
                        Text(stringResource(if (h.status == HabitStatus.ACTIVE)
                            R.string.habit_pause else R.string.habit_resume)) }
                    TextButton({ vm.lifecycle(HabitLifecycleDraft(h.id, HabitStatus.EXCLUDED, listId = h.listId)) },
                        enabled = !state.busy) { Text(stringResource(R.string.habit_exclude)) }
                } else TextButton({ vm.lifecycle(HabitLifecycleDraft(h.id, HabitStatus.ACTIVE,
                    listId = h.listId.takeIf { id -> lists.any { it.id == id } },
                    requiresDestination = h.listId != null && lists.none { it.id == h.listId })) },
                    enabled = !state.busy) {
                    Text(stringResource(R.string.habit_restore)) }
            }
        }, confirmButton = { TextButton({ vm.detail(null) }, enabled = !state.busy) {
            Text(stringResource(R.string.close)) } })
    }
    state.lifecycle?.let { d ->
        AlertDialog(onDismissRequest = { if (!state.busy) vm.lifecycle(null) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HabitError(state, vm::retry)
                Text(stringResource(R.string.habit_lifecycle_hint))
                if (d.status == HabitStatus.ACTIVE) HabitLists(lists, d.listId, !state.busy) {
                    vm.lifecycle(d.copy(listId = it, destinationChosen = true)) }
            }
        }, confirmButton = { TextButton(vm::confirmLifecycle, enabled = !state.busy) {
            Text(stringResource(R.string.habit_confirm_change)) } },
            dismissButton = { TextButton({ vm.lifecycle(null) }, enabled = !state.busy) {
                Text(stringResource(R.string.cancel)) } })
    }
}

