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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.ethos.app.core.ui.components.WeekBars
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
        Text(com.ethos.app.core.ui.format.EthosDates.monthTitle(month), style = MaterialTheme.typography.titleMedium)
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
fun HabitPracticeFeedback(state: HabitsUiState, undo: () -> Unit, dismiss: () -> Unit) {
    val last = state.lastPractice ?: return
    Snackbar(Modifier.padding(12.dp),
        action = { TextButton(undo, enabled = !state.busy) { Text(stringResource(R.string.habit_undo_quick)) } },
        dismissAction = { TextButton(dismiss) { Text(stringResource(R.string.habit_feedback_close)) } },
    ) {
        Text(stringResource(if (last.done) R.string.habit_marked_today else R.string.habit_unmarked_today))
    }
}

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
    open: (String) -> Unit, retry: () -> Unit, record: ((String) -> Unit)? = null,
) {
    val row: @Composable (Habit, HabitPeriod?) -> Unit = { h, period ->
        HabitRow(h, period, state.busy, { open(h.id) },
            quick = record?.let { r -> quickAction(state, h, period) { r(h.id) } },
            week = if (page == HabitPage.TODAY) null else weekHits(state, h),
            todayCount = if (page == HabitPage.TODAY) state.snapshot.records.count {
                it.habitId == h.id && it.date == state.today && !it.deleted } else null)
    }
    val habits = state.snapshot.habits.filter { h -> when (page) {
        HabitPage.HISTORY -> true
        HabitPage.LIST -> h.status != HabitStatus.EXCLUDED && h.listId == listId
        HabitPage.TODAY -> h.status == HabitStatus.ACTIVE
        HabitPage.REVIEW -> state.periods.any { it.habitId == h.id && it.outcome == PeriodOutcome.PENDING }
        else -> h.status != HabitStatus.EXCLUDED
    } }
    item("habit-errors") { HabitError(state, retry) }
    if (state.loading) item("habit-loading") { Text(stringResource(R.string.habit_loading)) }
    if (page != HabitPage.TODAY && page != HabitPage.REVIEW && page != HabitPage.HISTORY) item("new-habit") {
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
            row(h, period)
        }
    } else {
        val visible = habits.filter { h -> page != HabitPage.TODAY || state.periods.any {
            it.habitId == h.id && state.today in it.start..it.end
        } }
        items(visible.filter { it.status != HabitStatus.EXCLUDED },
            key = { "habit:${it.id}" }) { h ->
            row(h, state.periods.lastOrNull { it.habitId == h.id })
        }
        if (page == HabitPage.HISTORY || page == HabitPage.ALL) {
            item("excluded-habits-label") { Text(stringResource(R.string.habit_excluded)) }
            items(state.snapshot.habits.filter { it.status == HabitStatus.EXCLUDED },
                key = { "excluded:${it.id}" }) { h ->
                row(h, state.periods.lastOrNull { it.habitId == h.id })
            }
        }
    }
}

/** Quick action on a habit card; null when the habit cannot be recorded today. */
data class HabitQuick(val label: Int, val description: Int, val done: Boolean, val run: () -> Unit)

fun quickAction(state: HabitsUiState, h: Habit, period: HabitPeriod?, run: () -> Unit): HabitQuick? {
    if (h.status != HabitStatus.ACTIVE || !HabitRules.active(state.snapshot, h.id, state.today)) return null
    val type = (period?.rule ?: HabitRules.currentRule(state.snapshot, h.id, state.today))?.type ?: return null
    if (type != HabitType.PRACTICE) {
        return HabitQuick(R.string.habit_quick_occurrence, R.string.habit_quick_occurrence_description, false, run)
    }
    val done = state.snapshot.records.any { it.habitId == h.id && it.date == state.today && !it.deleted }
    return HabitQuick(if (done) R.string.habit_quick_done else R.string.habit_quick_record,
        if (done) R.string.habit_quick_unmark_description else R.string.habit_quick_record_description, done, run)
}

/** Monday to Sunday of the current week, true where a practice habit has records. */
fun weekHits(state: HabitsUiState, h: Habit): List<Boolean>? {
    if (HabitRules.currentRule(state.snapshot, h.id, state.today)?.type != HabitType.PRACTICE) return null
    val monday = HabitRules.start(state.today, HabitUnit.WEEK)
    return (0L..6L).map { d ->
        state.snapshot.records.any { it.habitId == h.id && !it.deleted && it.date == monday.plusDays(d) }
    }
}

private val habitColors = listOf(0xFF54745C, 0xFF426B96, 0xFF79568E, 0xFFA45B25, 0xFFA04D68, 0xFF657078)
private val habitIcons = listOf("❧", "▤", "⌂", "▣", "♡", "☆")

/** Model B habit card (spec 010). Avoid and limit habits use the warm amber container. */
@Composable
fun HabitRow(
    habit: Habit, period: HabitPeriod?, busy: Boolean, open: () -> Unit, quick: HabitQuick? = null,
    week: List<Boolean>? = null, todayCount: Int? = null,
) {
    val warm = period?.rule?.type?.let { it != HabitType.PRACTICE } == true
    val colors = MaterialTheme.colorScheme
    OutlinedCard(open, Modifier.fillMaxWidth(), enabled = !busy, shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (warm) colors.tertiaryContainer else colors.surfaceContainerLow),
        border = BorderStroke(1.dp, if (warm) colors.tertiaryContainer else colors.outlineVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(habitColors[habit.color])),
                    contentAlignment = Alignment.Center) { Text(habitIcons[habit.icon], color = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(habit.name, style = MaterialTheme.typography.titleSmall)
                    if (habit.status == HabitStatus.PAUSED) Text(stringResource(R.string.habit_paused),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    if (todayCount != null && warm) Text(
                        pluralStringResource(R.plurals.habit_today_occurrences, todayCount, todayCount),
                        style = MaterialTheme.typography.bodySmall)
                    if (todayCount == null) period?.let { p ->
                        val label = if (p.rule.type == HabitType.PRACTICE) R.string.habit_days_progress
                            else R.string.habit_events_progress
                        Text(stringResource(label, p.count, p.target), style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant)
                    }
                }
                quick?.let { q ->
                    val description = stringResource(q.description, habit.name)
                    val accent = if (warm) colors.tertiary else colors.primary
                    OutlinedButton(q.run,
                        Modifier.semantics { contentDescription = description }, enabled = !busy,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (warm) colors.tertiary else colors.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = when {
                                q.done -> colors.primary
                                warm -> Color.Transparent
                                else -> colors.secondaryContainer
                            },
                            contentColor = if (q.done) colors.onPrimary else accent),
                        contentPadding = PaddingValues(horizontal = 12.dp)) { Text(stringResource(q.label)) }
                }
            }
            if (todayCount == null) period?.let { p ->
                Text(stringResource(when (p.outcome) {
                    PeriodOutcome.SUCCESS -> R.string.habit_success
                    PeriodOutcome.FAILURE -> R.string.habit_failure
                    PeriodOutcome.PENDING -> R.string.habit_pending
                    else -> R.string.habit_in_progress
                }), Modifier.padding(start = 46.dp), style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
                if (p.excessIds.isNotEmpty()) Text(stringResource(R.string.habit_excess, p.excessIds.size),
                    Modifier.padding(start = 46.dp), style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
            }
            week?.let {
                WeekBars(it, stringResource(R.string.habit_week_description, it.count { hit -> hit }),
                    Modifier.padding(start = 46.dp))
            }
        }
    }
}

@Composable
fun HabitsContent(
    state: HabitsUiState, page: HabitPage, new: () -> Unit, open: (String) -> Unit, retry: () -> Unit,
    record: ((String) -> Unit)? = null,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        habitItems(state, page, null, new, open, retry, record)
    }
}

@Preview
@Composable
private fun HabitRowPreview() {
    com.ethos.app.core.ui.theme.EthosTheme(darkTheme = false) {
        HabitRow(Habit("preview", LocalDate.of(2026, 11, 6), "Ler algumas páginas", color = 1, icon = 1), null,
            false, {}, HabitQuick(R.string.habit_quick_record, R.string.habit_quick_record_description, false) {},
            listOf(true, true, false, true, false, false, false))
    }
}
