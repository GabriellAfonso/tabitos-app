package com.tabitos.app.features.statistics

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.accounting.domain.Progress
import com.tabitos.app.core.habits.domain.HabitUnit
import com.tabitos.app.core.timers.domain.TimerKind
import com.tabitos.app.core.timers.domain.TimerRules
import com.tabitos.app.core.ui.components.CoinAmount
import com.tabitos.app.core.ui.components.Eyebrow
import com.tabitos.app.core.ui.components.itemColor
import com.tabitos.app.core.ui.format.TabitosDates
import com.tabitos.app.core.ui.theme.TabitosTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class StatisticsTab(@param:StringRes val title: Int) {
    SUMMARY(R.string.statistics_summary_tab), HISTORY(R.string.statistics_history_tab),
}

/** Where a tapped item leads; the details themselves belong to the task and habit features. */
data class StatisticsNavigation(
    val openTask: (String) -> Unit, val openHabit: (String) -> Unit, val openRewards: () -> Unit,
)

data class StatisticsActions(
    val selectKind: (PeriodKind) -> Unit, val previous: () -> Unit, val next: () -> Unit,
    val selectFilter: (HistoryFilter) -> Unit, val selectItem: (StatisticsItem?) -> Unit,
    val selectTab: (StatisticsTab) -> Unit, val showMore: () -> Unit, val retry: () -> Unit,
    val openDeleted: (HistorySubject?) -> Unit, val askEraseConfirmation: () -> Unit, val confirmErase: () -> Unit,
    val viewDeletedHistory: () -> Unit,
    val navigation: StatisticsNavigation,
)

private val ptBr = Locale.forLanguageTag("pt-BR")
private val periodKinds = listOf(
    PeriodKind.WEEK to R.string.statistics_period_week, PeriodKind.MONTH to R.string.statistics_period_month,
    PeriodKind.YEAR to R.string.statistics_period_year, PeriodKind.ALL to R.string.statistics_period_all,
)
private const val MINUTES_PER_HOUR = 60
private const val MAX_LABELLED_BARS = 12
private val CHART_HEIGHT = 120.dp
private val MIN_BAR = 2.dp

@Composable
fun StatisticsContent(state: StatisticsUiState, today: LocalDate, zone: ZoneId, actions: StatisticsActions) {
    // Opens on Resumo (spec 022) unless a popup asked for an item's Histórico (spec 024).
    val tab = state.tab
    val content = state.content
    Column(Modifier.fillMaxSize()) {
        when {
            state.loading -> StatusMessage(R.string.statistics_loading) { CircularProgressIndicator() }
            state.error || content == null -> StatusMessage(R.string.statistics_error) {
                TextButton(actions.retry) { Text(stringResource(R.string.retry)) }
            }
            else -> {
                PeriodRow(content, today, actions)
                FilterRow(content, actions)
                PrimaryTabRow(selectedTabIndex = StatisticsTab.entries.indexOf(tab)) {
                    StatisticsTab.entries.forEach { entry ->
                        Tab(selected = tab == entry, onClick = { actions.selectTab(entry) },
                            text = { Text(stringResource(entry.title)) })
                    }
                }
                when (tab) {
                    StatisticsTab.SUMMARY -> SummaryTab(content, today, actions)
                    StatisticsTab.HISTORY -> HistoryTab(state, content, today, zone, actions)
                }
            }
        }
    }
    DeletedDialogs(state, actions)
}

@Composable
private fun StatusMessage(@StringRes text: Int, action: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(text))
        action()
    }
}

/** `‹ Esta semana ▾ ›`: the name opens the kind menu; "Tudo" has no arrows (spec 022, prototype P1). */
@Composable
private fun PeriodRow(content: StatisticsContentState, today: LocalDate, actions: StatisticsActions) {
    var menu by remember { mutableStateOf(false) }
    val label = periodText(content.label, today)
    val showsArrows = content.period.kind != PeriodKind.ALL
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (showsArrows) ArrowButton(R.drawable.ic_chevron_left, R.string.statistics_previous, content.hasPrevious,
            actions.previous)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val description = stringResource(R.string.statistics_choose_period, label)
            TextButton({ menu = true }, Modifier.semantics { contentDescription = description }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Icon(painterResource(R.drawable.ic_keyboard_arrow_down), contentDescription = null)
                    }
                    (content.label as? PeriodLabel.All)?.let {
                        Text(stringResource(R.string.statistics_since, TabitosDates.shortPlain(it.since, today)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            DropdownMenu(menu, { menu = false }) {
                periodKinds.forEach { (kind, title) ->
                    DropdownMenuItem(text = { Text(stringResource(title)) },
                        onClick = { menu = false; actions.selectKind(kind) },
                        trailingIcon = {
                            if (content.period.kind == kind) Icon(painterResource(R.drawable.ic_check), null)
                        })
                }
            }
        }
        if (showsArrows) ArrowButton(R.drawable.ic_chevron_right, R.string.statistics_next, content.hasNext,
            actions.next)
    }
}

@Composable
private fun ArrowButton(icon: Int, @StringRes label: Int, enabled: Boolean, onClick: () -> Unit) {
    val description = stringResource(label)
    IconButton(onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = description }) {
        Icon(painterResource(icon), contentDescription = null)
    }
}

@Composable
private fun periodText(label: PeriodLabel, today: LocalDate): String = when (label) {
    PeriodLabel.ThisWeek -> stringResource(R.string.statistics_this_week)
    PeriodLabel.LastWeek -> stringResource(R.string.statistics_last_week)
    is PeriodLabel.WeekRange -> stringResource(R.string.statistics_week_range,
        TabitosDates.shortPlain(label.start, today), TabitosDates.shortPlain(label.end, today))
    is PeriodLabel.Month -> "${label.month.month.getDisplayName(TextStyle.FULL_STANDALONE, ptBr)
        .replaceFirstChar { it.titlecase(ptBr) }} ${label.month.year}"
    is PeriodLabel.Year -> label.year.toString()
    is PeriodLabel.All -> stringResource(R.string.statistics_period_all)
}

@Composable
private fun SummaryTab(content: StatisticsContentState, today: LocalDate, actions: StatisticsActions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content.tasks?.let { TasksCard(it, today) }
        content.habits?.let { summary -> HabitsCard(summary) { actions.selectItem(StatisticsItem.Habit(it)) } }
        content.time?.let { TimeCard(it) }
        content.coins?.let { CoinsCard(it) }
    }
}

@Composable
private fun SummaryCard(@StringRes title: Int, body: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            body()
        }
    }
}

@Composable
private fun StatTile(value: String, @StringRes label: Int, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun percentText(percent: Int?): String =
    percent?.let { stringResource(R.string.statistics_percent, it) } ?: stringResource(R.string.statistics_no_percent)

@Composable
private fun TasksCard(summary: TaskSummary, today: LocalDate) = SummaryCard(R.string.statistics_tasks) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(summary.completed.toString(), R.string.statistics_completed, Modifier.weight(1f))
        StatTile(percentText(summary.onTimePercent), R.string.statistics_on_time, Modifier.weight(1f))
        StatTile(summary.postponed.toString(), R.string.statistics_postponed, Modifier.weight(1f))
    }
    if (summary.completed == 0) {
        Text(stringResource(R.string.statistics_tasks_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else BarChart(summary.buckets, today)
}

/** Completed tasks per bucket; each bar is announced as text for TalkBack. */
@Composable
private fun BarChart(buckets: List<Bucket>, today: LocalDate) {
    val highest = buckets.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Row(Modifier.fillMaxWidth().height(CHART_HEIGHT), horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom) {
        buckets.forEach { bucket ->
            val description = pluralStringResource(R.plurals.statistics_bar_description, bucket.count,
                bucketText(bucket, today), bucket.count)
            val color = if (bucket.future || bucket.count == 0) MaterialTheme.colorScheme.outlineVariant
                else MaterialTheme.colorScheme.primary
            Box(Modifier.weight(1f).fillMaxHeight().clearAndSetSemantics { contentDescription = description },
                contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.fillMaxWidth().fillMaxHeight(bucket.count.toFloat() / highest)
                    .heightIn(min = MIN_BAR).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(color))
            }
        }
    }
    if (buckets.size <= MAX_LABELLED_BARS) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            buckets.forEach { bucket ->
                Text(bucketAxis(bucket), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    textAlign = TextAlign.Center)
            }
        }
    }
}

private val weekdayAxis = DateTimeFormatter.ofPattern("EEEEE", ptBr)

private fun bucketAxis(bucket: Bucket): String = when (bucket.unit) {
    BucketUnit.DAY -> bucket.start.format(weekdayAxis).uppercase(ptBr)
    BucketUnit.MONTH -> bucket.start.month.getDisplayName(TextStyle.NARROW_STANDALONE, ptBr).uppercase(ptBr)
    BucketUnit.YEAR -> bucket.start.year.toString()
}

private fun bucketText(bucket: Bucket, today: LocalDate): String = when (bucket.unit) {
    BucketUnit.DAY -> TabitosDates.shortWithWeekday(bucket.start, today)
    BucketUnit.MONTH -> TabitosDates.monthTitle(YearMonth.from(bucket.start))
    BucketUnit.YEAR -> bucket.start.year.toString()
}

/** Tapping a habit focuses the page on it (spec 024). */
@Composable
private fun HabitsCard(summary: HabitSummary, focus: (String) -> Unit) = SummaryCard(R.string.statistics_habits) {
    if (summary.items.isEmpty()) {
        Text(stringResource(R.string.statistics_habits_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return@SummaryCard
    }
    Text(stringResource(R.string.statistics_habits_overall, percentText(summary.overallPercent)),
        style = MaterialTheme.typography.bodyLarge)
    summary.items.forEach { habit -> HabitRow(habit) { focus(habit.id) } }
}

@Composable
private fun HabitRow(habit: HabitStat, onClick: () -> Unit) {
    val rate = percentText(habit.ratePercent)
    val description = stringResource(R.string.statistics_habit_description, habit.name, rate, habit.streak)
    // Bad habits read in red everywhere (spec 020).
    val tone = if (habit.bad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
        .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(itemColor(habit.color)))
        Text(habit.name, Modifier.weight(1f), color = tone, maxLines = 2)
        Text(stringResource(R.string.statistics_habit_streak, habit.streak), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(rate, color = tone, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun durationText(millis: Long): String {
    val (hours, minutes) = TimerRules.minutesText(millis)
    return if (hours > 0) stringResource(R.string.statistics_hours_minutes, hours, minutes)
        else stringResource(R.string.statistics_minutes, minutes)
}

@Composable
private fun TimeCard(summary: TimeSummary) = SummaryCard(R.string.statistics_time) {
    if (summary.items.isEmpty()) {
        Text(stringResource(R.string.statistics_time_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return@SummaryCard
    }
    Text(stringResource(R.string.statistics_time_total, durationText(summary.totalMillis)),
        style = MaterialTheme.typography.bodyLarge)
    summary.items.forEach { item ->
        Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(item.title, Modifier.weight(1f), maxLines = 2)
            Text(durationText(item.millis), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CoinsCard(summary: CoinSummary) = SummaryCard(R.string.statistics_coins) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CoinTile(summary.earned, R.string.statistics_coins_earned, Modifier.weight(1f))
        CoinTile(summary.spent, R.string.statistics_coins_spent, Modifier.weight(1f))
    }
    val progress = summary.progress
    Text(stringResource(R.string.finance_level, progress.level), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.finance_progress_compact, progress.within, progress.required),
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LinearProgressIndicator(progress = { progress.within.toFloat() / progress.required.toFloat() },
        modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.outlineVariant,
        drawStopIndicator = {})
}

@Composable
private fun CoinTile(amount: Long, @StringRes label: Int, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        CoinAmount(amount.toString(), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val filterTitles = mapOf(
    HistoryFilter.ALL to R.string.statistics_filter_all, HistoryFilter.TASKS to R.string.statistics_filter_tasks,
    HistoryFilter.HABITS to R.string.statistics_filter_habits,
    HistoryFilter.BAD_HABITS to R.string.statistics_filter_bad_habits,
    HistoryFilter.TIME to R.string.statistics_filter_time,
    HistoryFilter.COINS to R.string.statistics_filter_coins,
)

/** Type chips shared by both tabs, then the focused item's chip when there is one (spec 024). */
@Composable
private fun FilterRow(content: StatisticsContentState, actions: StatisticsActions) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content.filters.forEach { filter ->
                FilterChip(content.filter == filter, { actions.selectFilter(filter) },
                    label = { Text(stringResource(filterTitles.getValue(filter))) })
            }
        }
        content.focused?.let { FocusedChip(it) { actions.selectItem(null) } }
    }
}

/** The item the page is focused on; ✕ goes back to its whole type (spec 024). */
@Composable
private fun FocusedChip(focused: FocusedItem, clear: () -> Unit) {
    val title = if (focused.deleted) stringResource(R.string.statistics_option_deleted, focused.title)
        else focused.title
    val description = stringResource(R.string.statistics_clear_item, title)
    InputChip(selected = true, onClick = clear, label = { Text(title, maxLines = 1) },
        modifier = Modifier.semantics { contentDescription = description },
        trailingIcon = { Icon(painterResource(R.drawable.ic_close), contentDescription = null, Modifier.size(18.dp)) })
}

@Composable
private fun HistoryTab(
    state: StatisticsUiState, content: StatisticsContentState, today: LocalDate, zone: ZoneId,
    actions: StatisticsActions,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        if (content.history.isEmpty()) {
            item(key = "empty") {
                Text(stringResource(R.string.statistics_history_empty), Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(content.history, key = { row -> rowKey(row) }) { row ->
            when (row) {
                is HistoryRow.MonthHeader -> Text(TabitosDates.monthTitle(row.month).replaceFirstChar {
                    it.titlecase(ptBr) }, Modifier.padding(top = 20.dp).semantics { heading() },
                    style = MaterialTheme.typography.titleMedium)
                is HistoryRow.DayHeader -> Eyebrow(TabitosDates.longPlain(row.day, today),
                    Modifier.padding(top = 16.dp, bottom = 4.dp).semantics { heading() })
                is HistoryRow.Item -> {
                    HistoryItemRow(row.item, state.gameMode, today, zone) { tapped(row.item.subject, actions) }
                    HorizontalDivider()
                }
            }
        }
        if (content.hasMore) {
            item(key = "more") {
                OutlinedButton(actions.showMore, Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(stringResource(R.string.statistics_show_more))
                }
            }
        }
        item(key = "bottom") { Spacer(Modifier.height(24.dp)) }
    }
}

private fun rowKey(row: HistoryRow): String = when (row) {
    is HistoryRow.MonthHeader -> "month:${row.month}"
    is HistoryRow.DayHeader -> "day:${row.day}"
    is HistoryRow.Item -> row.item.id
}

/** Existing items open their details; deleted ones offer erasing their history instead. */
private fun tapped(subject: HistorySubject, actions: StatisticsActions): Unit = when (subject) {
    is HistorySubject.Task -> if (subject.exists) actions.navigation.openTask(subject.occurrenceId)
        else actions.openDeleted(subject)
    is HistorySubject.Habit -> if (subject.exists) actions.navigation.openHabit(subject.habitId)
        else actions.openDeleted(subject)
    is HistorySubject.Reward -> actions.navigation.openRewards()
    is HistorySubject.Erased, HistorySubject.None -> Unit
}

private val HistorySubject.tappable: Boolean get() = this is HistorySubject.Task || this is HistorySubject.Habit ||
    this is HistorySubject.Reward

private val HistorySubject.deleted: Boolean get() =
    (this is HistorySubject.Task && !exists) || (this is HistorySubject.Habit && !exists)

@Composable
private fun HistoryItemRow(item: HistoryItem, gameMode: Boolean, today: LocalDate, zone: ZoneId, onClick: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // Adjustments stay visible but discreet: smaller, muted text (spec 022 FR-015).
    val titleStyle = if (item.adjustment) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
    val title = itemTitle(item)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .then(if (item.subject.tappable) Modifier.clickable(onClick = onClick) else Modifier)
        .semantics(mergeDescendants = true) {}
        .padding(vertical = if (item.adjustment) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(if (item.subject.deleted) stringResource(R.string.statistics_item_deleted, title) else title,
                style = titleStyle, color = if (item.adjustment) muted else MaterialTheme.colorScheme.onSurface)
            Text(detailLine(item, today, zone), style = MaterialTheme.typography.bodySmall, color = muted)
        }
        if (gameMode && item.coins != null) Column(horizontalAlignment = Alignment.End) {
            CoinAmount(signed(item.coins), style = MaterialTheme.typography.titleSmall)
            if (item.xp != null && item.xp != 0L) Text(stringResource(R.string.history_xp, signed(item.xp)),
                style = MaterialTheme.typography.bodySmall, color = muted)
        }
    }
}

@Composable
private fun itemTitle(item: HistoryItem): String = when (val subject = item.subject) {
    is HistorySubject.Erased -> stringResource(if (subject.task) R.string.statistics_deleted_task
        else R.string.statistics_deleted_habit)
    else -> item.title
}

@Composable
private fun detailLine(item: HistoryItem, today: LocalDate, zone: ZoneId): String {
    fun date(value: LocalDate?) = value?.let { TabitosDates.shortPlain(it, today) }.orEmpty()
    val what = when (item.kind) {
        HistoryKind.TASK_COMPLETED -> if (item.from != null && item.from != item.day)
            stringResource(R.string.statistics_item_task_completed_on, date(item.from))
            else stringResource(R.string.statistics_item_task_completed)
        HistoryKind.TASK_NOT_DONE -> stringResource(R.string.statistics_item_task_not_done)
        HistoryKind.TASK_MISSED -> stringResource(R.string.statistics_item_task_missed, date(item.from))
        HistoryKind.TASK_ADVANCED -> stringResource(R.string.statistics_item_task_advanced, date(item.from),
            date(item.to))
        HistoryKind.TASK_ADVANCE_UNDONE -> stringResource(R.string.statistics_item_task_advance_undone, date(item.to))
        HistoryKind.TASK_POSTPONED -> stringResource(R.string.statistics_item_task_postponed, date(item.from),
            date(item.to))
        HistoryKind.TASK_POSTPONE_UNDONE -> stringResource(R.string.statistics_item_task_postpone_undone,
            date(item.to))
        HistoryKind.TASK_UNDONE -> stringResource(R.string.statistics_item_task_undone)
        HistoryKind.HABIT_DONE -> stringResource(R.string.statistics_item_habit_done)
        HistoryKind.HABIT_OCCURRENCE -> stringResource(R.string.statistics_item_habit_occurrence)
        HistoryKind.HABIT_DAY_OK -> stringResource(R.string.statistics_item_habit_ok)
        HistoryKind.HABIT_DAY_OVER -> stringResource(R.string.statistics_item_habit_over)
        HistoryKind.TIMER -> stringResource(R.string.statistics_item_timer, durationText(item.millis ?: 0))
        HistoryKind.COINS -> stringResource(ledgerKindText(item.ledgerKind))
    }
    // Habit items sit on the day they refer to; their recording moment would only confuse.
    val timed = item.kind.isTask || item.kind == HistoryKind.COINS
    if (!timed) return what
    val moment = Instant.ofEpochMilli(item.moment).atZone(zone)
    return stringResource(R.string.history_line, what, TabitosDates.time(moment.hour * MINUTES_PER_HOUR + moment.minute))
}

private fun ledgerKindText(kind: String?): Int = when (kind) {
    LEDGER_REVERSAL -> R.string.history_kind_reversal
    LEDGER_REAPPLY -> R.string.history_kind_reapply
    LEDGER_CORRECTION -> R.string.history_kind_correction
    LEDGER_PENALTY -> R.string.history_kind_penalty
    LEDGER_PURCHASE -> R.string.history_kind_purchase
    else -> R.string.history_kind_gain
}

private fun signed(value: Long): String = if (value > 0) "+$value" else value.toString()

/** Deleted item → "Apagar histórico" → irreversible confirmation. */
@Composable
private fun DeletedDialogs(state: StatisticsUiState, actions: StatisticsActions) {
    if (state.deleted == null) return
    val error: @Composable () -> Unit = {
        if (state.eraseFailed) Text(stringResource(R.string.statistics_erase_error),
            color = MaterialTheme.colorScheme.error)
    }
    val close = { if (!state.erasing) actions.openDeleted(null) }
    if (!state.confirmingErase) AlertDialog(onDismissRequest = close,
        title = { Text(stringResource(R.string.statistics_deleted_title)) },
        text = { Text(stringResource(R.string.statistics_deleted_body)) },
        confirmButton = { TextButton(actions.askEraseConfirmation) {
            Text(stringResource(R.string.statistics_erase)) } },
        dismissButton = { Row {
            TextButton(actions.viewDeletedHistory) { Text(stringResource(R.string.view_history)) }
            TextButton(close) { Text(stringResource(R.string.cancel)) }
        } })
    else AlertDialog(onDismissRequest = close,
        title = { Text(stringResource(R.string.statistics_erase_confirm_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            error()
            Text(stringResource(R.string.statistics_erase_confirm_body))
        } },
        confirmButton = { TextButton(actions.confirmErase, enabled = !state.erasing) {
            Text(stringResource(R.string.statistics_erase), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(close, enabled = !state.erasing) { Text(stringResource(R.string.cancel)) } })
}

private val previewToday: LocalDate = LocalDate.of(2026, 11, 17)

private fun previewState(): StatisticsUiState {
    val monday = StatisticsPeriod.weekStart(previewToday)
    val buckets = (0L..6L).map { Bucket(monday.plusDays(it), BucketUnit.DAY, listOf(2, 3, 0, 1, 0, 0, 0)[it.toInt()],
        future = monday.plusDays(it) > previewToday) }
    val completed = HistoryItem("a", 0, previewToday, HistoryKind.TASK_COMPLETED,
        HistorySubject.Task("t:1", "t", exists = true), "Pagar a conta de luz", coins = 10, xp = 20)
    val postponed = HistoryItem("b", 0, previewToday, HistoryKind.TASK_POSTPONED,
        HistorySubject.Task("t:2", "t2", exists = false), "Ligar para o dentista", from = previewToday.minusDays(1),
        to = previewToday.plusDays(2))
    return StatisticsUiState(loading = false, content = StatisticsContentState(
        period = StatisticsPeriod.current(PeriodKind.WEEK, previewToday), label = PeriodLabel.ThisWeek,
        hasPrevious = true, hasNext = false,
        tasks = TaskSummary(6, 80, 1, buckets),
        habits = HabitSummary(listOf(
            HabitStat("h1", "Ler 20 páginas", 1, 0, bad = false, ratePercent = 86, streak = 5, unit = HabitUnit.DAY),
            HabitStat("h2", "Sem refrigerante", 6, 0, bad = true, ratePercent = 71, streak = 2, unit = HabitUnit.DAY),
        ), overallPercent = 78),
        time = TimeSummary(5_400_000, listOf(TimeStat(TimerKind.HABIT, "h1", "Ler 20 páginas", 5_400_000))),
        coins = CoinSummary(120, 50, Progress(3, 40, 200)),
        filters = historyFilters(gameMode = true),
        history = historyRows(listOf(completed, postponed), monthHeaders = false), hasMore = false,
    ))
}

private val previewActions = StatisticsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    StatisticsNavigation({}, {}, {}))

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun StatisticsSummaryPreview() {
    TabitosTheme { StatisticsContent(previewState(), previewToday, ZoneId.of("America/Sao_Paulo"), previewActions) }
}
