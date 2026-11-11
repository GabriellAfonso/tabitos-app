package com.ethos.app.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ethos.app.R
import com.ethos.app.core.habits.domain.Habit
import com.ethos.app.core.habits.domain.HabitStatus
import com.ethos.app.core.tasks.domain.TaskFields
import com.ethos.app.core.tasks.domain.TaskOccurrence
import com.ethos.app.core.tasks.domain.TaskSnapshot
import com.ethos.app.core.ui.theme.EthosTheme
import com.ethos.app.core.ui.components.SegmentedControl
import androidx.compose.foundation.shape.CircleShape
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

data class CalendarActions(
    val view: (CalendarView) -> Unit = {}, val select: (LocalDate) -> Unit = {}, val shift: (Long) -> Unit = {},
    val today: () -> Unit = {}, val filters: (CalendarFilters) -> Unit = {}, val retry: () -> Unit = {},
    val openTask: (String) -> Unit = {}, val openHabit: (String) -> Unit = {},
    val fill: (String, LocalDate) -> Unit = { _, _ -> }, val openEntry: (String) -> Unit = {},
)

@Composable
fun CalendarScreen(
    vm: CalendarViewModel, openTask: (String) -> Unit, openHabit: (String) -> Unit,
    fill: (String, LocalDate) -> Unit, openEntry: (String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { vm.refresh(); delay(60_000) } }
    }
    CalendarContent(state, CalendarActions(vm::view, vm::select, vm::shift, vm::goToday, vm::filters, vm::retry,
        openTask, openHabit, fill, openEntry))
}

private val locale = Locale.forLanguageTag("pt-BR")
@Composable
private fun title(view: CalendarView, anchor: LocalDate): String = when (view) {
    CalendarView.MONTH -> anchor.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", locale))
    CalendarView.WEEK -> CalendarModel.weekStart(anchor).let {
        stringResource(R.string.calendar_week_range, it.format(DateTimeFormatter.ofPattern("dd/MM", locale)),
            it.plusDays(6).format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale))) }
    CalendarView.DAY -> anchor.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", locale))
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        content()
    }
}

@Composable
fun CalendarContent(state: CalendarUiState, actions: CalendarActions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.error) OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(R.string.task_storage_error), color = MaterialTheme.colorScheme.error)
                TextButton(actions.retry) { Text(stringResource(R.string.retry)) }
            }
        }
        if (state.loading) Text(stringResource(R.string.loading_tasks))
        Text(title(state.view, state.anchor).replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.headlineSmall)
        Row {
            TextButton({ actions.shift(-1) }) { Text(stringResource(R.string.calendar_previous)) }
            TextButton(actions.today) { Text(stringResource(R.string.calendar_go_today)) }
            TextButton({ actions.shift(1) }) { Text(stringResource(R.string.calendar_next)) }
        }
        SegmentedControl(CalendarView.entries, state.view, { stringResource(when (it) {
            CalendarView.MONTH -> R.string.calendar_month
            CalendarView.WEEK -> R.string.calendar_week
            CalendarView.DAY -> R.string.calendar_day
        }) }, actions.view)
        Filters(state, actions)
        if (state.view != CalendarView.DAY) Grid(state, actions)
        state.selected?.let { DayDetails(it, state.filters, actions) }
    }
}

@Composable
private fun Filters(state: CalendarUiState, actions: CalendarActions) {
    val f = state.filters
    Text(stringResource(R.string.calendar_types), style = MaterialTheme.typography.labelLarge)
    ChipRow {
        listOf(CalendarType.TASKS to R.string.tasks, CalendarType.HABITS to R.string.habits,
            CalendarType.TRACKERS to R.string.calendar_trackers).forEach { (type, label) ->
            FilterChip(type in f.types, {
                actions.filters(f.copy(types = if (type in f.types) f.types - type else f.types + type))
            }, { Text(stringResource(label)) })
        }
    }
    Text(stringResource(R.string.calendar_list), style = MaterialTheme.typography.labelLarge)
    ChipRow {
        FilterChip(f.list == CalendarFilters.ALL_LISTS, { actions.filters(f.copy(list = CalendarFilters.ALL_LISTS)) },
            { Text(stringResource(R.string.calendar_all_lists)) })
        FilterChip(f.list == CalendarFilters.INBOX, { actions.filters(f.copy(list = CalendarFilters.INBOX)) },
            { Text(stringResource(R.string.inbox)) })
        state.sources.tasks.activeLists.forEach { l ->
            FilterChip(f.list == l.id, { actions.filters(f.copy(list = l.id)) }, { Text(l.name) })
        }
    }
    val habits = state.sources.habits.habits.filter { it.status != HabitStatus.EXCLUDED || it.id == f.habitId }
    if (habits.isNotEmpty()) {
        Text(stringResource(R.string.calendar_habit), style = MaterialTheme.typography.labelLarge)
        ChipRow {
            FilterChip(f.habitId == null, { actions.filters(f.copy(habitId = null)) },
                { Text(stringResource(R.string.calendar_all_habits)) })
            habits.sortedBy { it.name }.forEach { h ->
                FilterChip(f.habitId == h.id, { actions.filters(f.copy(habitId = h.id)) }, { Text(h.name) })
            }
        }
    }
}

@Composable
private fun Grid(state: CalendarUiState, actions: CalendarActions) {
    val labels = stringResource(R.string.calendar_weekdays).split(',')
    val selectedLabel = stringResource(R.string.calendar_selected)
    val minimum = 300.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    // Fits the screen normally; scrolls sideways only when large fonts need more room.
    val width = maxOf(maxWidth, minimum)
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row(Modifier.width(width)) {
            labels.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall) }
        }
        state.cells.chunked(7).forEach { week ->
            Row(Modifier.width(width).padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { cell ->
                    val d = cell.day
                    val selected = d.date == state.anchor
                    val base = if (d.entries.isEmpty() && CalendarType.TRACKERS in state.filters.types &&
                        d.tasks.isEmpty() && d.habits.isEmpty())
                        stringResource(R.string.calendar_cell_no_data, d.date.toString())
                        else stringResource(R.string.calendar_cell, d.date.toString(), d.tasks.size,
                            d.habitRecordCount, d.entries.size)
                    val description = if (selected) "$base · $selectedLabel" else base
                    val shape = RoundedCornerShape(12.dp)
                    Column(Modifier.weight(1f).heightIn(min = 52.dp)
                        .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
                        .background(if (d.date == state.today) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.background, shape)
                        .clickable(role = Role.Button) { actions.select(d.date) }
                        .semantics { contentDescription = description; this.selected = selected }
                        .padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(d.date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium,
                            color = if (cell.inPeriod) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outline)
                        val count = d.tasks.size + d.habitRecordCount
                        if (count > 0) Text(count.toString(), style = MaterialTheme.typography.labelSmall)
                        // Discreet tracker indicator; absence is never drawn as a value.
                        if (d.entries.isNotEmpty()) Box(Modifier.padding(top = 3.dp).size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape))
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun DayDetails(day: CalendarDay, filters: CalendarFilters, actions: CalendarActions) {
    HorizontalDivider()
    Text(day.date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", locale)),
        style = MaterialTheme.typography.titleMedium)
    if (day.empty && day.fills.isEmpty()) Text(stringResource(R.string.calendar_nothing))
    if (day.tasks.isNotEmpty()) {
        Text(stringResource(R.string.tasks), style = MaterialTheme.typography.titleSmall)
        day.tasks.forEach { o ->
            OutlinedCard({ actions.openTask(o.id) }, Modifier.fillMaxWidth()) {
                Text(o.fields.title, Modifier.padding(12.dp))
            }
        }
    }
    if (day.habits.isNotEmpty()) {
        Text(stringResource(R.string.habits), style = MaterialTheme.typography.titleSmall)
        day.habits.forEach { r ->
            OutlinedCard({ actions.openHabit(r.habit.id) }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.calendar_habit_records, r.habit.name, r.count), Modifier.padding(12.dp))
            }
        }
    }
    if (CalendarType.TRACKERS in filters.types) {
        Text(stringResource(R.string.calendar_trackers), style = MaterialTheme.typography.titleSmall)
        if (day.entries.isEmpty()) Text(stringResource(R.string.tracker_no_data))
        day.entries.forEach { e ->
            val time = Instant.ofEpochMilli(e.entry.filledAt).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
            OutlinedCard({ actions.openEntry(e.entry.id) }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.calendar_tracker_entry, e.habit.name, time), Modifier.padding(12.dp))
            }
        }
        day.fills.forEach { f ->
            OutlinedButton({ if (f.existing != null) actions.openEntry(f.existing.id) else actions.fill(f.tracker.id,
                day.date) }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.calendar_tracker_fill, f.habit.name))
            }
        }
    }
}

@Preview
@Composable
private fun CalendarPreview() {
    val today = LocalDate.of(2026, 11, 7)
    val sources = CalendarSources(TaskSnapshot(occurrences = listOf(TaskOccurrence("o", "t", 1,
        TaskFields(title = "Revisar orçamento", deadline = com.ethos.app.core.tasks.domain.DeadlineKind.DAY),
        today, today, today))), com.ethos.app.core.habits.domain.HabitSnapshot(habits = listOf(
        Habit("h", today, "Caminhar"))))
    val f = CalendarFilters()
    EthosTheme {
        Surface { CalendarContent(CalendarUiState(sources, today, loading = false,
            cells = CalendarModel.cells(sources, f, CalendarView.MONTH, today, today),
            selected = CalendarModel.day(sources, f, today, today)), CalendarActions()) }
    }
}
