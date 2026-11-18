package com.ethos.app.features.habits

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.habits.domain.ALL_WEEKDAYS
import com.ethos.app.core.habits.domain.Habit
import com.ethos.app.core.habits.domain.HabitRule
import com.ethos.app.core.habits.domain.HabitStreak
import com.ethos.app.core.habits.domain.HabitType
import com.ethos.app.core.habits.domain.HabitUnit
import com.ethos.app.core.tasks.domain.TaskList
import com.ethos.app.core.ui.components.DetailHistoryLink
import com.ethos.app.core.ui.components.DetailIconAction
import com.ethos.app.core.ui.components.DetailSummaryItem
import com.ethos.app.core.ui.components.ExpandableDescription
import com.ethos.app.core.ui.components.NumberStepper
import com.ethos.app.core.ui.components.itemColors
import com.ethos.app.core.ui.format.EthosDates
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalDate

/** What the habit detail popup can ask for (spec 023); grouped because the popup has many actions. */
data class HabitDetailActions(
    val close: () -> Unit,
    val setPractice: (LocalDate, Boolean) -> Unit,
    val editDay: (LocalDate) -> Unit,
    val addOccurrence: (LocalDate) -> Unit,
    val removeOccurrence: (LocalDate) -> Unit,
    val openTimer: () -> Unit,
    val edit: () -> Unit,
    val reminders: () -> Unit,
    val tracker: () -> Unit,
    val pause: () -> Unit,
    val resume: () -> Unit,
    val exclude: () -> Unit,
    val restore: () -> Unit,
    val erase: () -> Unit,
    /** Opens Estatísticas filtered by this habit (spec 024); null hides the link. */
    val history: (() -> Unit)? = null,
)

/**
 * Habit details in the shape of the task popup (spec 023, option "B · Com a semana"): summary, the last 7 days,
 * the main action and labelled icons. No close button: it closes on outside tap or back.
 */
@Composable
fun HabitDetail(
    model: HabitDetailModel, list: TaskList?, today: LocalDate, busy: Boolean,
    error: @Composable () -> Unit, actions: HabitDetailActions,
) {
    val habit = model.habit
    AlertDialog(onDismissRequest = { if (!busy) actions.close() }, confirmButton = {},
        title = { HabitDetailTitle(habit) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                error()
                HabitSummaryLine(model.rule, list)
                if (habit.description.isNotBlank()) ExpandableDescription(habit.description)
                model.streak?.let { HabitStatsLine(it, model.rate) }
                if (model.main != HabitDetailMain.EXCLUDED) {
                    HabitWeekStrip(model, today, busy, actions)
                    model.progress?.let { HabitProgressMeter(it) }
                }
                HabitMainArea(model, busy, actions)
                if (model.main != HabitDetailMain.EXCLUDED) HabitActionIcons(model.main, busy, actions)
                actions.history?.let { DetailHistoryLink(!busy, it) }
            }
        })
}

@Composable
private fun HabitDetailTitle(habit: Habit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(itemColors[habit.color]),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(habit.iconDrawable), contentDescription = null, tint = Color.White)
        }
        Text(habit.name, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitSummaryLine(rule: HabitRule, list: TaskList?) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DetailSummaryItem(R.drawable.ic_repeat, frequencyLabel(rule))
        DetailSummaryItem(R.drawable.ic_format_list_bulleted, list?.name ?: stringResource(R.string.inbox),
            tint = list?.let { itemColors[it.color] })
    }
}

@Composable
private fun frequencyLabel(rule: HabitRule): String = when (rule.type) {
    HabitType.PRACTICE -> when (rule.unit) {
        HabitUnit.DAY -> stringResource(if (rule.weekdays == ALL_WEEKDAYS) R.string.habit_freq_every_day
            else R.string.habit_chosen_days)
        HabitUnit.WEEK -> pluralStringResource(R.plurals.habit_freq_week, rule.target, rule.target)
        HabitUnit.MONTH -> pluralStringResource(R.plurals.habit_freq_month, rule.target, rule.target)
    }
    HabitType.AVOID -> stringResource(R.string.habit_avoid)
    HabitType.LIMIT -> stringResource(when (rule.unit) {
        HabitUnit.DAY -> R.string.habit_freq_limit_day
        HabitUnit.WEEK -> R.string.habit_freq_limit_week
        HabitUnit.MONTH -> R.string.habit_freq_limit_month
    }, rule.target)
}

@Composable
private fun HabitStatsLine(streak: HabitStreak, rate: Int?) {
    Text(if (rate == null) stringResource(R.string.habit_detail_stats_no_rate, streak.current, streak.best)
        else stringResource(R.string.habit_detail_stats, streak.current, streak.best, rate),
        style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun HabitWeekStrip(model: HabitDetailModel, today: LocalDate, busy: Boolean, actions: HabitDetailActions) {
    val weekdays = stringResource(R.string.calendar_weekdays).split(',')
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            model.week.forEach { day ->
                val label = if (day.date == today) stringResource(R.string.habit_today)
                    else weekdays[day.date.dayOfWeek.value - 1]
                val onTap = {
                    if (model.breaking) actions.editDay(day.date)
                    else actions.setPractice(day.date, day.count == 0)
                }
                HabitWeekCell(day, label, isToday = day.date == today, spoken = daySpoken(day, today),
                    enabled = day.editable && !busy, onClick = onTap)
            }
        }
        Text(stringResource(if (model.breaking) R.string.habit_week_hint_breaking else R.string.habit_week_hint),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun daySpoken(day: HabitWeekDay, today: LocalDate): String {
    val date = EthosDates.shortWithWeekday(day.date, today)
    return when (day.mark) {
        HabitDayMark.DONE -> stringResource(R.string.habit_day_done, date)
        HabitDayMark.MISSED -> stringResource(R.string.habit_day_missed, date)
        HabitDayMark.CLEAN -> stringResource(R.string.habit_day_clean, date)
        HabitDayMark.COUNTED -> pluralStringResource(R.plurals.habit_day_count, day.count, date, day.count)
        HabitDayMark.OVER_LIMIT -> pluralStringResource(R.plurals.habit_day_over, day.count, date, day.count)
        HabitDayMark.OPEN -> stringResource(R.string.habit_day_open, date)
    }
}

@Composable
private fun RowScope.HabitWeekCell(
    day: HabitWeekDay, label: String, isToday: Boolean, spoken: String, enabled: Boolean, onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.weight(1f).heightIn(min = 72.dp).alpha(if (day.editable) 1f else CLOSED_DAY_ALPHA)
        .clip(RoundedCornerShape(14.dp))
        .border(BorderStroke(1.dp, if (isToday) colors.primary else colors.outlineVariant), RoundedCornerShape(14.dp))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .clearAndSetSemantics { contentDescription = spoken }
        .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1,
            overflow = TextOverflow.Clip)
        Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall)
        HabitDayBadge(day)
    }
}

@Composable
private fun HabitDayBadge(day: HabitWeekDay) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (day.mark) {
        HabitDayMark.DONE -> colors.primary to colors.onPrimary
        HabitDayMark.COUNTED -> colors.errorContainer to colors.onErrorContainer
        HabitDayMark.OVER_LIMIT -> colors.error to colors.onError
        HabitDayMark.MISSED -> Color.Transparent to colors.error
        HabitDayMark.CLEAN -> Color.Transparent to colors.primary
        HabitDayMark.OPEN -> Color.Transparent to colors.outline
    }
    val ring = if (day.mark == HabitDayMark.OPEN) Modifier.border(2.dp, colors.outline, CircleShape) else Modifier
    Box(Modifier.size(24.dp).clip(CircleShape).background(container).then(ring), contentAlignment = Alignment.Center) {
        when (day.mark) {
            HabitDayMark.DONE, HabitDayMark.CLEAN ->
                Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp), tint = content)
            HabitDayMark.MISSED -> Icon(painterResource(R.drawable.ic_close), null, Modifier.size(16.dp), tint = content)
            HabitDayMark.COUNTED, HabitDayMark.OVER_LIMIT ->
                Text(day.count.toString(), style = MaterialTheme.typography.labelMedium, color = content)
            HabitDayMark.OPEN -> Unit
        }
    }
}

@Composable
private fun HabitProgressMeter(progress: HabitDetailProgress) {
    val text = if (progress.limit) stringResource(when (progress.unit) {
        HabitUnit.DAY -> R.string.habit_progress_limit_day
        HabitUnit.WEEK -> R.string.habit_progress_limit_week
        HabitUnit.MONTH -> R.string.habit_progress_limit_month
    }, progress.count, progress.target) else stringResource(when (progress.unit) {
        HabitUnit.MONTH -> R.string.habit_progress_days_month
        else -> R.string.habit_progress_days_week
    }, progress.count, progress.target)
    val over = progress.limit && progress.count > progress.target
    val fraction = if (progress.target == 0) (if (progress.count > 0) 1f else 0f)
        else (progress.count.toFloat() / progress.target).coerceAtMost(1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(progress = { fraction }, Modifier.fillMaxWidth(),
            color = when {
                over -> MaterialTheme.colorScheme.error
                progress.limit -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            })
    }
}

@Composable
private fun HabitMainArea(model: HabitDetailModel, busy: Boolean, actions: HabitDetailActions) {
    val today = model.week.last().date
    when (model.main) {
        HabitDetailMain.MARK -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ actions.setPractice(today, true) }, Modifier.weight(1f), enabled = !busy) {
                ButtonIcon(R.drawable.ic_check)
                Text(stringResource(R.string.habit_mark_today), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            model.timerMinutes?.let { minutes ->
                val spoken = pluralStringResource(R.plurals.task_open_timer, minutes, minutes)
                FilledTonalButton(actions.openTimer, Modifier.weight(1f).semantics { contentDescription = spoken },
                    enabled = !busy) {
                    ButtonIcon(R.drawable.ic_play_arrow)
                    Text(stringResource(R.string.task_timer_minutes, minutes), maxLines = 1)
                }
            }
        }
        HabitDetailMain.DONE_TODAY -> StatusStrip(stringResource(R.string.habit_done_today), R.drawable.ic_check,
            MaterialTheme.colorScheme.secondaryContainer, R.string.habit_unmark, busy) {
            actions.setPractice(today, false) }
        HabitDetailMain.BREAKING -> BreakingArea(model, busy, actions)
        HabitDetailMain.PAUSED -> StatusStrip(stringResource(R.string.habit_paused_strip), R.drawable.ic_pause,
            MaterialTheme.colorScheme.tertiaryContainer, R.string.habit_resume_short, busy, actions.resume)
        HabitDetailMain.EXCLUDED -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusStrip(stringResource(R.string.habit_excluded_strip), R.drawable.ic_delete,
                MaterialTheme.colorScheme.surfaceContainerHighest, R.string.habit_restore, busy, actions.restore)
            TextButton(actions.erase, enabled = !busy) {
                Text(stringResource(R.string.habit_erase_completely), color = MaterialTheme.colorScheme.error)
            }
        }
        HabitDetailMain.UNAVAILABLE -> Unit
    }
}

@Composable
private fun BreakingArea(model: HabitDetailModel, busy: Boolean, actions: HabitDetailActions) {
    val today = model.week.last().date
    val avoid = model.rule.type == HabitType.AVOID
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ actions.addOccurrence(today) }, Modifier.fillMaxWidth(), enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError)) {
            ButtonIcon(R.drawable.ic_add)
            Text(stringResource(if (avoid) R.string.habit_register_relapse else R.string.habit_register_use),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val count = model.todayCount
        if (count > 0) StatusStrip(
            pluralStringResource(if (avoid) R.plurals.habit_today_relapses else R.plurals.habit_today_uses, count, count),
            null, MaterialTheme.colorScheme.errorContainer, R.string.habit_undo_one, busy) {
            actions.removeOccurrence(today) }
        else if (avoid) Text(stringResource(R.string.habit_today_clean), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ButtonIcon(@DrawableRes icon: Int) {
    Icon(painterResource(icon), null, Modifier.size(18.dp))
    Spacer(Modifier.width(6.dp))
}

@Composable
private fun StatusStrip(
    text: String, @DrawableRes icon: Int?, container: Color, @StringRes action: Int, busy: Boolean,
    onAction: () -> Unit,
) {
    Surface(color = container, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(painterResource(it), null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onAction, enabled = !busy) { Text(stringResource(action)) }
        }
    }
}

@Composable
private fun HabitActionIcons(main: HabitDetailMain, busy: Boolean, actions: HabitDetailActions) {
    val paused = main == HabitDetailMain.PAUSED
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        DetailIconAction(R.drawable.ic_edit, R.string.edit, !busy, actions.edit)
        DetailIconAction(R.drawable.ic_notification_reminder, R.string.reminders, !busy, actions.reminders)
        DetailIconAction(R.drawable.ic_checklist, R.string.habit_track, !busy, actions.tracker)
        if (paused) DetailIconAction(R.drawable.ic_play_arrow, R.string.habit_resume_short, !busy, actions.resume)
        else DetailIconAction(R.drawable.ic_pause, R.string.habit_pause_short, !busy, actions.pause)
        DetailIconAction(R.drawable.ic_delete, R.string.delete, !busy, actions.exclude)
    }
}

/** "−/+" sheet that corrects how many times a bad habit happened on one open day (spec 023). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDaySheet(
    model: HabitDetailModel, date: LocalDate, today: LocalDate, busy: Boolean,
    add: () -> Unit, remove: () -> Unit, dismiss: () -> Unit,
) {
    val count = model.week.firstOrNull { it.date == date }?.count ?: 0
    val avoid = model.rule.type == HabitType.AVOID
    val over = count > model.rule.target
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(EthosDates.shortWithWeekday(date, today), style = MaterialTheme.typography.titleMedium)
            Text(if (avoid) stringResource(R.string.habit_day_sheet_relapses)
                else stringResource(R.string.habit_day_sheet_limit, model.rule.target),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            NumberStepper(count, { if (it > count) add() else remove() }, range = 0..MAX_DAY_COUNT,
                suffix = pluralStringResource(R.plurals.habit_times, count), enabled = !busy,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Surface(color = if (over) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(if (over) R.string.habit_day_sheet_over else R.string.habit_day_sheet_within),
                    Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(dismiss, Modifier.align(Alignment.End)) { Text(stringResource(R.string.habit_day_sheet_done)) }
        }
    }
}

/** Faded look of days whose period already closed. */
private const val CLOSED_DAY_ALPHA = 0.5f
/** Upper bound of the day sheet stepper; only limits the control, not the rules. */
private const val MAX_DAY_COUNT = 99

@Preview
@Composable
private fun HabitDetailPreview() {
    val today = LocalDate.of(2026, 11, 17)
    val habit = Habit("p", today.minusDays(30), "Ler 20 páginas", description = "Antes de dormir.", color = 1, icon = 1)
    val week = (6L downTo 0).map { back ->
        HabitWeekDay(today.minusDays(back), if (back == 3L) HabitDayMark.MISSED
            else if (back == 0L) HabitDayMark.OPEN else HabitDayMark.DONE, if (back == 3L || back == 0L) 0 else 1,
            editable = back <= 2)
    }
    EthosTheme {
        HabitDetail(HabitDetailModel(habit, HabitRule("r", "p", habit.created), HabitStreak(2, 14, false, HabitUnit.DAY),
            83, week, HabitDetailMain.MARK, 0, null, timerMinutes = 10), list = null, today = today, busy = false,
            error = {}, HabitDetailActions({}, { _, _ -> }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}))
    }
}
