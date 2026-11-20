package com.tabitos.app.features.calendar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.tabitos.app.R
import com.tabitos.app.core.habits.domain.Habit
import com.tabitos.app.core.habits.domain.HabitRecord
import com.tabitos.app.core.habits.domain.HabitSnapshot
import com.tabitos.app.core.icons.AppIconTables
import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.TaskFields
import com.tabitos.app.core.tasks.domain.TaskOccurrence
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import com.tabitos.app.core.ui.components.ChoiceSheet
import com.tabitos.app.core.ui.components.SegmentedControl
import com.tabitos.app.core.ui.components.itemColors
import com.tabitos.app.core.ui.components.option
import com.tabitos.app.core.ui.format.DateText
import com.tabitos.app.core.ui.format.TabitosDates
import com.tabitos.app.core.ui.format.label
import com.tabitos.app.core.ui.theme.TabitosTheme
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
    val openDay: (LocalDate) -> Unit = {},
)

private const val REFRESH_MILLIS = 60_000L
private val ptBr: Locale = Locale.forLanguageTag("pt-BR")
private val weekdayShort = DateTimeFormatter.ofPattern("EEE", ptBr)
private val clockTime = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun CalendarScreen(
    vm: CalendarViewModel, openTask: (String) -> Unit, openHabit: (String) -> Unit,
    fill: (String, LocalDate) -> Unit, openEntry: (String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    // Keeps "today" right when the page stays open across midnight.
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { vm.refresh(); delay(REFRESH_MILLIS) }
        }
    }
    CalendarContent(state, CalendarActions(
        view = vm::view, select = vm::select, shift = vm::shift, today = vm::goToday, filters = vm::filters,
        retry = vm::retry, openTask = openTask, openHabit = openHabit, fill = fill, openEntry = openEntry,
        openDay = vm::openDay,
    ))
}

/** Option C of `prototypes/calendario`: compact grid on top, only the chosen day's list scrolls (spec 030). */
@Composable
fun CalendarContent(state: CalendarUiState, actions: CalendarActions) {
    var showsFilters by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        if (state.error) StorageError(actions.retry)
        if (state.loading) Text(stringResource(R.string.loading_tasks), Modifier.padding(horizontal = 20.dp))
        CalendarToolbar(state, actions, openFilters = { showsFilters = true })
        PeriodRow(CalendarPresentation.periodLabel(state.view, state.anchor, state.today), actions.shift)
        ActiveFilterChips(state, actions.filters)
        when (state.view) {
            CalendarView.MONTH -> MonthView(state, actions)
            CalendarView.WEEK -> WeekAgenda(state, actions)
            CalendarView.DAY -> DayView(state, actions)
        }
    }
    if (showsFilters) FilterSheet(state, actions.filters, close = { showsFilters = false })
}

@Composable
private fun StorageError(retry: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.task_storage_error), color = MaterialTheme.colorScheme.error)
            TextButton(retry) { Text(stringResource(R.string.retry)) }
        }
    }
}

/** Mês | Semana | Dia centered; "Ir para hoje" keeps its slot so the control never shifts. */
@Composable
private fun CalendarToolbar(state: CalendarUiState, actions: CalendarActions, openFilters: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (CalendarPresentation.showsTodayButton(state.view, state.anchor, state.today)) {
                IconAction(R.drawable.ic_today, stringResource(R.string.calendar_go_today), actions.today)
            }
        }
        Box(Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            SegmentedControl(CalendarView.entries, state.view, { stringResource(viewLabel(it)) }, actions.view,
                Modifier.widthIn(max = 320.dp))
        }
        FilterButton(CalendarPresentation.isActive(state.filters), openFilters)
    }
}

private fun viewLabel(view: CalendarView): Int = when (view) {
    CalendarView.MONTH -> R.string.calendar_month
    CalendarView.WEEK -> R.string.calendar_week
    CalendarView.DAY -> R.string.calendar_day
}

@Composable
private fun FilterButton(active: Boolean, onClick: () -> Unit) {
    val description = stringResource(if (active) R.string.calendar_filters_active else R.string.calendar_filters)
    IconButton(onClick, Modifier.semantics { contentDescription = description }) {
        BadgedBox(badge = { if (active) Badge() }) {
            Icon(painterResource(R.drawable.ic_filter_list), contentDescription = null)
        }
    }
}

@Composable
private fun IconAction(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick, Modifier.semantics { contentDescription = description }) {
        Icon(painterResource(icon), contentDescription = null)
    }
}

@Composable
private fun PeriodRow(label: String, shift: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        IconAction(R.drawable.ic_chevron_left, stringResource(R.string.calendar_previous)) { shift(-1) }
        Text(label, Modifier.padding(horizontal = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconAction(R.drawable.ic_chevron_right, stringResource(R.string.calendar_next)) { shift(1) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterChips(state: CalendarUiState, change: (CalendarFilters) -> Unit) {
    val chips = CalendarPresentation.activeFilters(state.filters, state.sources.tasks.activeLists,
        state.sources.habits.habits)
    if (chips.isEmpty()) return
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        chips.forEach { chip ->
            val text = activeFilterText(chip)
            val description = stringResource(R.string.calendar_remove_filter, text)
            InputChip(selected = true, onClick = { change(CalendarPresentation.without(state.filters, chip)) },
                label = { Text(text) }, modifier = Modifier.semantics { contentDescription = description },
                trailingIcon = { Icon(painterResource(R.drawable.ic_close), null, Modifier.size(18.dp)) })
        }
    }
}

@Composable
private fun activeFilterText(filter: ActiveFilter): String = when (filter) {
    is ActiveFilter.Types -> if (filter.shown.isEmpty()) stringResource(R.string.calendar_no_types)
        else filter.shown.map { stringResource(typeLabel(it)) }.joinToString(", ")
    is ActiveFilter.TaskListFilter -> filter.name ?: stringResource(
        if (filter.inbox) R.string.inbox else R.string.calendar_removed_list)
    is ActiveFilter.HabitFilter -> filter.name
}

private fun typeLabel(type: CalendarType): Int = when (type) {
    CalendarType.TASKS -> R.string.tasks
    CalendarType.HABITS -> R.string.habits
    CalendarType.TRACKERS -> R.string.calendar_trackers
}

@Composable
private fun MonthView(state: CalendarUiState, actions: CalendarActions) {
    Column(Modifier.fillMaxSize()) {
        MonthGrid(state, actions.select)
        GridLegend()
        HorizontalDivider()
        state.selected?.let { day ->
            DayTitle(day.date, state.today, Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                DayItems(day, state, actions)
            }
        }
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, select: (LocalDate) -> Unit) {
    val labels = stringResource(R.string.calendar_weekdays).split(',')
    val minimum = 300.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        // Fits the screen normally; scrolls sideways only when large fonts need more room.
        val width = maxOf(maxWidth, minimum)
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row(Modifier.width(width)) {
                labels.forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.cells.chunked(DAYS_IN_WEEK).forEach { week ->
                Row(Modifier.width(width), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { cell ->
                        DayCell(cell, cell.day.date == state.anchor, cell.day.date == state.today, state.filters,
                            Modifier.weight(1f)) { select(cell.day.date) }
                    }
                }
            }
        }
    }
}

private const val DAYS_IN_WEEK = 7

@Composable
private fun DayCell(
    cell: CalendarCell, selected: Boolean, isToday: Boolean, filters: CalendarFilters, modifier: Modifier,
    onClick: () -> Unit,
) {
    val day = cell.day
    val colors = MaterialTheme.colorScheme
    val base = if (day.announcesNoData(filters)) stringResource(R.string.calendar_cell_no_data, day.date.toString())
        else stringResource(R.string.calendar_cell, day.date.toString(), day.tasks.size, day.habitRecordCount,
            day.entries.size)
    val description = if (selected) "$base · ${stringResource(R.string.calendar_selected)}" else base
    Column(modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
        .background(if (selected) colors.primaryContainer else Color.Transparent)
        .clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description; this.selected = selected }
        .padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.size(28.dp).background(if (isToday) colors.primary else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center) {
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || selected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> colors.onPrimary
                    cell.inPeriod -> colors.onSurface
                    else -> colors.outline
                })
        }
        DayMarks(day)
    }
}

/** Tasks, habit records and tracker entries each get their own mark, named by [GridLegend]. */
@Composable
private fun DayMarks(day: CalendarDay) {
    Row(Modifier.heightIn(min = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (day.hasTasks) Mark(MarkKind.TASKS)
        if (day.hasHabitRecords) Mark(MarkKind.HABITS)
        if (day.hasTrackerEntry) Mark(MarkKind.TRACKER)
    }
}

private enum class MarkKind { TASKS, HABITS, TRACKER }

@Composable
private fun Mark(kind: MarkKind) {
    val colors = MaterialTheme.colorScheme
    val modifier = Modifier.size(6.dp)
    when (kind) {
        MarkKind.TASKS -> Box(modifier.background(colors.primary, CircleShape))
        MarkKind.HABITS -> Box(modifier.background(colors.onSurfaceVariant, CircleShape))
        MarkKind.TRACKER -> Box(modifier.border(1.5.dp, colors.tertiary, CircleShape))
    }
}

@Composable
private fun GridLegend() {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
        listOf(MarkKind.TASKS to R.string.calendar_legend_tasks, MarkKind.HABITS to R.string.calendar_legend_habits,
            MarkKind.TRACKER to R.string.calendar_legend_trackers).forEach { (kind, label) ->
            Row(Modifier.padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Mark(kind)
                Text(stringResource(label), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DayTitle(date: LocalDate, today: LocalDate, modifier: Modifier = Modifier) {
    val plain = TabitosDates.longPlain(date, today)
    val title = when (val relative = TabitosDates.short(date, today)) {
        is DateText.Plain -> plain.replaceFirstChar { it.titlecase(ptBr) }
        else -> stringResource(R.string.calendar_relative_day, relative.label(), plain)
    }
    Text(title, modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun DayView(state: CalendarUiState, actions: CalendarActions) {
    val day = state.selected ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp)) {
        DayTitle(day.date, state.today)
        DayItems(day, state, actions)
    }
}

@Composable
private fun DayItems(day: CalendarDay, state: CalendarUiState, actions: CalendarActions) {
    val lists = state.sources.tasks.activeLists
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (day.empty && day.newFills.isEmpty()) {
            val nothing = if (CalendarPresentation.isActive(state.filters)) R.string.calendar_nothing
                else R.string.calendar_nothing_plain
            Text(stringResource(nothing), Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (day.tasks.isNotEmpty()) {
            SectionTitle(R.string.tasks)
            day.tasks.forEach { task ->
                TaskRow(task, CalendarPresentation.taskMeta(task, lists, state.today)) { actions.openTask(task.id) }
            }
        }
        if (day.habits.isNotEmpty()) {
            SectionTitle(R.string.habits)
            day.habits.forEach { record -> HabitRecordRow(record) { actions.openHabit(record.habit.id) } }
        }
        if (CalendarType.TRACKERS in state.filters.types && day.tracked) TrackerSection(day, actions)
    }
}

@Composable
private fun SectionTitle(label: Int) {
    Text(stringResource(label), Modifier.padding(top = 6.dp).semantics { heading() },
        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ItemCard(
    onClick: () -> Unit, warning: Boolean = false, content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (warning) colors.errorContainer else colors.surfaceContainerLow),
        border = BorderStroke(1.dp, if (warning) colors.errorContainer else colors.outlineVariant)) {
        Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }
}

@Composable
private fun TaskRow(task: TaskOccurrence, meta: TaskMeta, open: () -> Unit) {
    ItemCard(open) {
        Column(Modifier.weight(1f)) {
            Text(task.fields.title, style = MaterialTheme.typography.bodyLarge)
            val colors = MaterialTheme.colorScheme
            Text(taskMetaText(meta), style = MaterialTheme.typography.bodySmall,
                color = if (meta.overdue) colors.error else colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun taskMetaText(meta: TaskMeta): String {
    val list = meta.listName ?: stringResource(if (meta.inbox) R.string.inbox else R.string.calendar_removed_list)
    val overdue = if (meta.overdue) stringResource(R.string.calendar_task_overdue) else null
    return listOfNotNull(list, meta.time, overdue).joinToString(" · ")
}

@Composable
private fun HabitRecordRow(record: HabitDayRecords, open: () -> Unit) {
    ItemCard(open, warning = record.breaking) {
        HabitTile(record.habit)
        Column(Modifier.weight(1f)) {
            Text(record.habit.name, style = MaterialTheme.typography.bodyLarge)
            val summary = if (record.breaking) {
                pluralStringResource(R.plurals.calendar_habit_slips, record.count, record.count)
            } else pluralStringResource(R.plurals.calendar_habit_practice, record.count, record.count)
            Text(summary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HabitTile(habit: Habit) {
    Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(itemColors[habit.color]),
        contentAlignment = Alignment.Center) {
        Icon(painterResource(AppIconTables.habit[habit.icon].option.icon), contentDescription = null,
            tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TrackerSection(day: CalendarDay, actions: CalendarActions) {
    SectionTitle(R.string.calendar_trackers)
    day.entries.forEach { entry ->
        ItemCard({ actions.openEntry(entry.entry.id) }) {
            Text(stringResource(R.string.calendar_tracker_entry, entry.habit.name, filledTime(entry)),
                Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.calendar_tracker_edit), color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge)
        }
    }
    day.newFills.forEach { fill ->
        ItemCard({ actions.fill(fill.tracker.id, day.date) }) {
            Column(Modifier.weight(1f)) {
                Text(fill.habit.name, style = MaterialTheme.typography.bodyLarge)
                if (day.entries.none { it.tracker.id == fill.tracker.id }) {
                    Text(stringResource(R.string.tracker_no_data), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FilledTonalButton({ actions.fill(fill.tracker.id, day.date) }) {
                Text(stringResource(R.string.calendar_tracker_fill_short))
            }
        }
    }
    if (day.entries.isEmpty() && day.newFills.isEmpty()) {
        Text(stringResource(R.string.tracker_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun filledTime(entry: TrackerDayEntry): String =
    Instant.ofEpochMilli(entry.entry.filledAt).atZone(ZoneId.systemDefault()).format(clockTime)

/** Seven days, Monday first; the day number opens that day alone. */
@Composable
private fun WeekAgenda(state: CalendarUiState, actions: CalendarActions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(start = 12.dp, end = 16.dp, bottom = 24.dp)) {
        state.cells.forEachIndexed { index, cell ->
            if (index > 0) HorizontalDivider()
            AgendaDay(cell.day, state, actions)
        }
    }
}

@Composable
private fun AgendaDay(day: CalendarDay, state: CalendarUiState, actions: CalendarActions) {
    val isToday = day.date == state.today
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val description = stringResource(R.string.calendar_open_day, TabitosDates.longPlain(day.date, state.today))
        Column(Modifier.width(48.dp).clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) { actions.openDay(day.date) }
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day.date.format(weekdayShort).removeSuffix("."), style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant)
            Box(Modifier.size(34.dp).background(if (isToday) colors.primary else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center) {
                Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium,
                    color = if (isToday) colors.onPrimary else colors.onSurface)
            }
        }
        Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AgendaItems(day, state, actions)
        }
    }
}

@Composable
private fun AgendaItems(day: CalendarDay, state: CalendarUiState, actions: CalendarActions) {
    val colors = MaterialTheme.colorScheme
    val lists = state.sources.tasks.activeLists
    day.tasks.forEach { task ->
        val meta = CalendarPresentation.taskMeta(task, lists, state.today)
        Surface({ actions.openTask(task.id) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
            color = if (meta.overdue) colors.errorContainer else colors.secondaryContainer) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(task.fields.title, style = MaterialTheme.typography.bodyMedium)
                Text(taskMetaText(meta), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (day.habits.isNotEmpty()) AgendaLine(MarkKind.HABITS, day.habits.joinToString(", ") { it.habit.name })
    if (CalendarType.TRACKERS in state.filters.types && day.tracked) {
        val entryFormat = stringResource(R.string.calendar_tracker_entry)
        val text = if (day.entries.isEmpty()) stringResource(R.string.tracker_no_data)
            else day.entries.joinToString(", ") { entryFormat.format(it.habit.name, filledTime(it)) }
        AgendaLine(MarkKind.TRACKER, text)
    }
    if (day.empty && !day.tracked) {
        Text(stringResource(R.string.calendar_agenda_empty), Modifier.padding(vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun AgendaLine(kind: MarkKind, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Mark(kind)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(state: CalendarUiState, change: (CalendarFilters) -> Unit, close: () -> Unit) {
    val f = state.filters
    ChoiceSheet(stringResource(R.string.calendar_filters), close) {
        SheetLabel(R.string.calendar_show)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalendarType.entries.forEach { type ->
                SheetChip(type in f.types, stringResource(typeLabel(type))) {
                    change(CalendarPresentation.toggleType(f, type))
                }
            }
        }
        SheetLabel(R.string.calendar_list)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SheetChip(f.list == CalendarFilters.ALL_LISTS, stringResource(R.string.calendar_all_lists)) {
                change(f.copy(list = CalendarFilters.ALL_LISTS))
            }
            SheetChip(f.list == CalendarFilters.INBOX, stringResource(R.string.inbox)) {
                change(f.copy(list = CalendarFilters.INBOX))
            }
            state.sources.tasks.activeLists.forEach { list ->
                SheetChip(f.list == list.id, list.name) { change(f.copy(list = list.id)) }
            }
        }
        val habits = CalendarPresentation.sheetHabits(state.sources.habits.habits, f)
        if (habits.isNotEmpty()) {
            SheetLabel(R.string.calendar_habit)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SheetChip(f.habitId == null, stringResource(R.string.calendar_all_habits)) {
                    change(f.copy(habitId = null))
                }
                habits.forEach { habit ->
                    SheetChip(f.habitId == habit.id, habit.name) { change(f.copy(habitId = habit.id)) }
                }
            }
            Text(stringResource(R.string.calendar_habit_hides_tasks), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton({ change(CalendarFilters()) }, enabled = CalendarPresentation.isActive(f)) {
            Text(stringResource(R.string.calendar_clear_filters))
        }
    }
}

@Composable
private fun SheetLabel(label: Int) {
    Spacer(Modifier.size(2.dp))
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SheetChip(selected: Boolean, text: String, onClick: () -> Unit) {
    FilterChip(selected, onClick, { Text(text) }, leadingIcon = if (selected) {
        { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp)) }
    } else null)
}

@Preview(widthDp = 380, heightDp = 820)
@Composable
private fun CalendarPreview() {
    val today = LocalDate.of(2026, 11, 17)
    val sources = CalendarSources(
        TaskSnapshot(occurrences = listOf(
            TaskOccurrence("o1", "t1", 1, TaskFields(title = "Enviar relatório mensal", deadline = DeadlineKind.DAY,
                date = today, minute = 17 * 60), today, today, today),
            TaskOccurrence("o2", "t2", 1, TaskFields(title = "Pagar conta de luz", deadline = DeadlineKind.DAY,
                date = today.plusDays(1)), today.plusDays(1), today.plusDays(1), today.plusDays(1)),
        )),
        HabitSnapshot(habits = listOf(Habit("h", today.minusDays(30), "Caminhar")),
            records = listOf(HabitRecord("r", "h", today, 1))),
    )
    val filters = CalendarFilters()
    TabitosTheme {
        Surface {
            CalendarContent(CalendarUiState(sources, today, loading = false,
                cells = CalendarModel.cells(sources, filters, CalendarView.MONTH, today, today),
                selected = CalendarModel.day(sources, filters, today, today)), CalendarActions())
        }
    }
}
