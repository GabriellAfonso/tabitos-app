package com.ethos.app.features.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.habits.domain.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HabitCalendar(records: List<HabitRecord>, today: LocalDate, onDate: (String) -> Unit) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthText)
    val calendarWidth = 364.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    Column {
        Column {
            TextButton({ monthText = month.minusMonths(1).toString() }) {
                Text(stringResource(R.string.habit_previous_month)) }
            TextButton({ monthText = month.plusMonths(1).toString() }) {
                Text(stringResource(R.string.habit_following_month)) }
        }
        Text(monthText, style = MaterialTheme.typography.titleMedium)
        val offset = month.atDay(1).dayOfWeek.value - 1
        val cells = (0 until offset).map { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        Column(Modifier.horizontalScroll(rememberScrollState())) {
        cells.chunked(7).forEach { week -> Row(Modifier.width(calendarWidth)) {
            week.forEach { date ->
                if (date == null) Spacer(Modifier.weight(1f)) else {
                    val count = records.count { it.date == date }
                    val label = stringResource(R.string.habit_calendar_day, date.toString(), count)
                    TextButton({ onDate(date.toString()) },
                        Modifier.weight(1f).semantics { contentDescription = label }) {
                        Text(if (count > 0) "${date.dayOfMonth}•" else date.dayOfMonth.toString())
                    }
                }
            }
            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
        } }
        }
    }
}

enum class HabitPage { ALL, TODAY, REVIEW, LIST, HISTORY }

@Composable
fun HabitError(state: HabitsUiState, retry: () -> Unit) {
    if (state.storageError || state.problem != null) OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(if (state.storageError) R.string.habit_storage_error else when (state.problem) {
                HabitProblem.DATE -> R.string.habit_date_error
                HabitProblem.INACTIVE -> R.string.habit_inactive_error
                HabitProblem.CAPACITY -> R.string.habit_capacity_error
                HabitProblem.PARTIAL_REQUIRED -> R.string.habit_partial_error
                HabitProblem.DESTINATION -> R.string.habit_destination_error
                else -> R.string.habit_input_error
            }), color = MaterialTheme.colorScheme.error)
            if (state.storageError) TextButton(retry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
fun HabitProgress(p: HabitPeriod) {
    val label = if (p.rule.type == HabitType.PRACTICE) R.string.habit_days_progress else R.string.habit_events_progress
    Text(stringResource(label, p.count, p.target))
    Text(stringResource(R.string.habit_period_range, p.start.toString(), p.end.toString()),
        style = MaterialTheme.typography.bodySmall)
    Text(stringResource(when (p.outcome) {
        PeriodOutcome.SUCCESS -> R.string.habit_success
        PeriodOutcome.FAILURE -> R.string.habit_failure
        PeriodOutcome.PENDING -> R.string.habit_pending
        else -> R.string.habit_in_progress
    }))
    if (p.excessIds.isNotEmpty()) Text(stringResource(R.string.habit_excess, p.excessIds.size))
}

fun LazyListScope.habitItems(
    state: HabitsUiState, page: HabitPage, listId: String?, new: () -> Unit,
    open: (String) -> Unit, retry: () -> Unit,
) {
    val habits = state.snapshot.habits.filter { h -> when (page) {
        HabitPage.HISTORY -> true
        HabitPage.LIST -> h.status != HabitStatus.EXCLUDED && h.listId == listId
        HabitPage.TODAY -> h.status == HabitStatus.ACTIVE
        HabitPage.REVIEW -> state.periods.any { it.habitId == h.id && it.outcome == PeriodOutcome.PENDING }
        else -> h.status != HabitStatus.EXCLUDED
    } }
    item("habit-errors") { HabitError(state, retry) }
    if (state.loading) item("habit-loading") { Text(stringResource(R.string.habit_loading)) }
    if (page != HabitPage.REVIEW && page != HabitPage.HISTORY) item("new-habit") {
        Button(new, enabled = !state.busy && !state.loading) { Text(stringResource(R.string.new_habit)) }
    }
    if (!state.loading && habits.isEmpty()) item("habit-empty") {
        Text(stringResource(R.string.habit_empty_real))
    }
    if (page == HabitPage.REVIEW) {
        val ids = habits.map { it.id }.toSet()
        items(state.periods.filter { it.habitId in ids && it.outcome == PeriodOutcome.PENDING },
            key = { "habit-review:${it.key}" }) { period ->
            val h = habits.first { it.id == period.habitId }
            HabitRow(h, period, state.busy) { open(h.id) }
        }
    } else {
        val completed = habits.filter { h -> page == HabitPage.TODAY && state.periods.any {
            it.habitId == h.id && state.today in it.start..it.end && it.rule.type == HabitType.PRACTICE &&
                it.rule.unit != HabitUnit.DAY && it.outcome == PeriodOutcome.SUCCESS
        } }.map { it.id }.toSet()
        val visible = habits.filter { h -> page != HabitPage.TODAY || state.periods.any {
            it.habitId == h.id && state.today in it.start..it.end
        } }
        items(visible.filter { it.id !in completed && it.status != HabitStatus.EXCLUDED },
            key = { "habit:${it.id}" }) { h ->
            HabitRow(h, state.periods.lastOrNull { it.habitId == h.id }, state.busy) { open(h.id) }
        }
        if (completed.isNotEmpty()) item("habit-met-goals") {
            var expanded by rememberSaveable { mutableStateOf(false) }
            Column {
                TextButton({ expanded = !expanded }) { Text(stringResource(R.string.habit_goals_met, completed.size)) }
                if (expanded) habits.filter { it.id in completed }.forEach { h ->
                    HabitRow(h, state.periods.lastOrNull { it.habitId == h.id }, state.busy) { open(h.id) }
                }
            }
        }
        if (page == HabitPage.HISTORY || page == HabitPage.ALL) {
            item("excluded-habits-label") { Text(stringResource(R.string.habit_excluded)) }
            items(state.snapshot.habits.filter { it.status == HabitStatus.EXCLUDED },
                key = { "excluded:${it.id}" }) { h ->
                HabitRow(h, state.periods.lastOrNull { it.habitId == h.id }, state.busy) { open(h.id) }
            }
        }
    }
}

@Composable
fun HabitRow(habit: Habit, period: HabitPeriod?, busy: Boolean, open: () -> Unit) {
    OutlinedCard(open, Modifier.fillMaxWidth(), enabled = !busy) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val colors = listOf(0xFF54745C, 0xFF426B96, 0xFF79568E, 0xFFA45B25, 0xFFA04D68, 0xFF657078)
            val icons = listOf("❧", "▤", "⌂", "▣", "♡", "☆")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(icons[habit.icon], color = Color(colors[habit.color]))
                Text(habit.name, style = MaterialTheme.typography.titleSmall)
            }
            if (habit.status == HabitStatus.PAUSED) Text(stringResource(R.string.habit_paused))
            period?.let { HabitProgress(it) }
        }
    }
}

@Composable
fun HabitsContent(state: HabitsUiState, page: HabitPage, new: () -> Unit, open: (String) -> Unit, retry: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        habitItems(state, page, null, new, open, retry)
    }
}

@Preview
@Composable
private fun HabitRowPreview() {
    MaterialTheme { HabitRow(Habit("preview", LocalDate.of(2026, 11, 6), "Ler algumas páginas"), null, false, {}) }
}
