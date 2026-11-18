package com.ethos.app.features.habits

import com.ethos.app.core.ui.timers.TimerChip
import com.ethos.app.core.ui.timers.TimerChipButton
import com.ethos.app.core.ui.theme.EthosTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
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
import com.ethos.app.core.ui.components.ContentFilter
import com.ethos.app.core.ui.components.WeekBars
import com.ethos.app.core.ui.components.itemColors
import com.ethos.app.core.habits.domain.*
import java.time.LocalDate

/** Where habit cards appear; [EXCLUDED] is the Settings sub-page that keeps excluded habits restorable. */
enum class HabitPage { TODAY, REVIEW, LIST, EXCLUDED }

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

/** Habits are chances, never debts: closed periods read "Feito"/"Perdido", the late window stays open. */
fun outcomeLabel(p: HabitPeriod, today: LocalDate): Int = when {
    p.outcome == PeriodOutcome.SUCCESS -> R.string.habit_success
    p.outcome == PeriodOutcome.FAILURE -> R.string.habit_failure
    p.end < today -> R.string.habit_late_window
    else -> R.string.habit_in_progress
}

fun LazyListScope.habitItems(
    state: HabitsUiState, page: HabitPage, listId: String?,
    open: (String) -> Unit, retry: () -> Unit, record: ((String) -> Unit)? = null,
    timerChip: (Habit) -> TimerChip? = { null }, filter: ContentFilter = ContentFilter.HABITS,
) {
    // Revisão is for tasks only (spec 014).
    if (page == HabitPage.REVIEW) return
    if (state.storageError || state.problem != null) item("habit-errors") { HabitError(state, retry) }
    if (state.loading) item("habit-loading") { Text(stringResource(R.string.habit_loading)) }
    val habits = visibleHabits(state, page, listId, filter.showsBadHabits)
    val showsEmptyMessage = page != HabitPage.TODAY || filter == ContentFilter.HABITS
    if (showsEmptyMessage && !state.loading && habits.isEmpty()) item("habit-empty") {
        Text(stringResource(if (page == HabitPage.EXCLUDED) R.string.habit_excluded_empty
            else R.string.habit_empty_real))
    }
    items(habits, key = { "habit:${it.id}" }) { h ->
        val period = state.periods.lastOrNull { it.habitId == h.id }
        HabitRow(h, period, state.busy, { open(h.id) }, today = state.today,
            consistency = state.streaks[h.id]?.current?.let { it to state.rates[h.id] }, timer = timerChip(h),
            quick = record?.let { r -> quickAction(state, h, period) { r(h.id) } },
            week = if (page == HabitPage.TODAY) null else weekHits(state, h),
            todayCount = if (page == HabitPage.TODAY) state.snapshot.records.count {
                it.habitId == h.id && it.date == state.today && !it.deleted } else null)
    }
}

/**
 * Hoje shows active habits whose period includes today, bad habits only when [showsBadHabits]; a list page its
 * own; Settings the excluded ones.
 */
internal fun visibleHabits(
    state: HabitsUiState, page: HabitPage, listId: String?, showsBadHabits: Boolean = true,
): List<Habit> =
    state.snapshot.habits.filter { h ->
        when (page) {
            HabitPage.TODAY -> h.status == HabitStatus.ACTIVE &&
                state.periods.any { it.habitId == h.id && state.today in it.start..it.end &&
                    (showsBadHabits || !it.isBadHabit) }
            HabitPage.LIST -> h.status != HabitStatus.EXCLUDED && h.listId == listId
            HabitPage.EXCLUDED -> h.status == HabitStatus.EXCLUDED
            HabitPage.REVIEW -> false
        }
    }

/** Avoid and limit periods ("Quebrar mau hábito"); practice is the good habit. */
private val HabitPeriod.isBadHabit: Boolean get() = rule?.type?.let { it != HabitType.PRACTICE } == true

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


/** Model B habit card (spec 010). Avoid and limit habits use the red error container so they read as "stop". */
@Composable
fun HabitRow(
    habit: Habit, period: HabitPeriod?, busy: Boolean, open: () -> Unit, quick: HabitQuick? = null,
    week: List<Boolean>? = null, todayCount: Int? = null, today: LocalDate,
    consistency: Pair<Int, Int?>? = null, timer: TimerChip? = null,
) {
    val breaking = period?.isBadHabit == true
    val colors = MaterialTheme.colorScheme
    OutlinedCard(open, Modifier.fillMaxWidth(), enabled = !busy, shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (breaking) colors.errorContainer else colors.surfaceContainerLow),
        border = BorderStroke(1.dp, if (breaking) colors.errorContainer else colors.outlineVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(itemColors[habit.color]),
                    contentAlignment = Alignment.Center) {
                    Icon(painterResource(habit.iconDrawable), contentDescription = null, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(habit.name, style = MaterialTheme.typography.titleSmall)
                    if (habit.status == HabitStatus.PAUSED) Text(stringResource(R.string.habit_paused),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    if (todayCount != null && breaking) Text(
                        pluralStringResource(R.plurals.habit_today_occurrences, todayCount, todayCount),
                        style = MaterialTheme.typography.bodySmall)
                    if (todayCount == null) period?.let { p ->
                        val label = if (p.rule.type == HabitType.PRACTICE) R.string.habit_days_progress
                            else R.string.habit_events_progress
                        Text(stringResource(label, p.count, p.target), style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant)
                    }
                }
                if (timer != null && quick?.done != true) TimerChipButton(timer, habit.name, !busy)
                quick?.let { q ->
                    val description = stringResource(q.description, habit.name)
                    val accent = if (breaking) colors.error else colors.primary
                    OutlinedButton(q.run,
                        Modifier.semantics { contentDescription = description }, enabled = !busy,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (breaking) colors.error else colors.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = when {
                                q.done -> colors.primary
                                breaking -> Color.Transparent
                                else -> colors.secondaryContainer
                            },
                            contentColor = if (q.done) colors.onPrimary else accent),
                        contentPadding = PaddingValues(horizontal = 12.dp)) { Text(stringResource(q.label)) }
                }
            }
            consistency?.let { (streak, rate) ->
                Text(if (rate == null) stringResource(R.string.habit_consistency_streak, streak)
                    else stringResource(R.string.habit_consistency, streak, rate),
                    Modifier.padding(start = 46.dp), style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
            }
            if (todayCount == null) period?.let { p ->
                Text(stringResource(outcomeLabel(p, today)), Modifier.padding(start = 46.dp),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
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
fun HabitsContent(state: HabitsUiState, page: HabitPage, open: (String) -> Unit, retry: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        habitItems(state, page, null, open, retry)
    }
}

@Preview
@Composable
private fun HabitRowPreview() {
    EthosTheme(darkTheme = false) {
        HabitRow(Habit("preview", LocalDate.of(2026, 11, 6), "Ler algumas páginas", color = 1, icon = 1), null,
            false, {}, HabitQuick(R.string.habit_quick_record, R.string.habit_quick_record_description, false) {},
            listOf(true, true, false, true, false, false, false), today = LocalDate.of(2026, 11, 6))
    }
}
