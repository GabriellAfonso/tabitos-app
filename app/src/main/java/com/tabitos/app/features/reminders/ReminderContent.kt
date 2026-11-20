package com.tabitos.app.features.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tabitos.app.R
import com.tabitos.app.core.reminders.domain.ReminderDefinition
import com.tabitos.app.core.reminders.domain.ReminderKind
import com.tabitos.app.core.reminders.domain.ReminderMode
import com.tabitos.app.core.ui.components.TabitosDatePickerDialog
import com.tabitos.app.core.ui.components.GrowingSheet
import com.tabitos.app.core.ui.components.ScrollableTimeField
import com.tabitos.app.core.ui.format.DateText
import com.tabitos.app.core.ui.format.TabitosDates
import com.tabitos.app.core.ui.format.label
import com.tabitos.app.core.ui.theme.TabitosTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Returns the in-context notification permission request and refreshes reminder capability on resume.
 * Asking only happens when a feature that needs it is configured (AGENTS: permissions).
 */
@Composable
fun rememberNotificationPermissionRequest(refresh: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { refresh() }
    }
    return {
        if (context.needsNotificationPermission()) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** Android 13+ asks for notifications at runtime; older versions grant them at install. */
internal fun Context.needsNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= 33 &&
    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

/** Callbacks of the reminders sheet opened from a task or habit detail. */
data class ReminderListActions(
    val close: () -> Unit,
    val scope: (Boolean) -> Unit,
    val edit: (ReminderDefinition?) -> Unit,
    val remove: (ReminderDefinition) -> Unit,
    val quickAdd: (Long) -> Unit,
    val retry: () -> Unit,
)

data class ReminderEditorActions(
    val change: (ReminderDraft?) -> Unit,
    val save: () -> Unit,
    val delete: () -> Unit,
    val retry: () -> Unit,
)

@Composable
fun ReminderDialogs(state: ReminderUiState, vm: ReminderViewModel, requestPermission: () -> Unit) {
    if (state.fallback) AlertDialog(onDismissRequest = { vm.fallback(false) },
        text = { Text(stringResource(R.string.reminder_gone)) },
        confirmButton = { TextButton({ vm.fallback(false) }) { Text(stringResource(R.string.close)) } })
    if (state.snoozeKey != null) SnoozeDialog(state, vm)
    when {
        state.draft != null -> ReminderEditor(state, ReminderEditorActions(change = vm::draft,
            save = { vm.save(requestPermission) }, delete = vm::removeDraft, retry = vm::retry))
        state.targetId != null -> ReminderListSheet(state, ReminderListActions(close = vm::close, scope = vm::scope,
            edit = vm::edit, remove = vm::remove, quickAdd = { vm.quickAdd(it, requestPermission) },
            retry = vm::retry))
    }
}

@Composable
private fun SnoozeDialog(state: ReminderUiState, vm: ReminderViewModel) {
    AlertDialog(onDismissRequest = vm::dismissSnooze,
        title = { Text(stringResource(R.string.reminder_snooze)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderErrors(state, vm::retry)
                SNOOZE_MINUTES.forEach { minute -> Button({ vm.snooze(minute) }, enabled = !state.busy) {
                    Text(pluralStringResource(R.plurals.reminder_minutes, minute, minute)) } }
                OutlinedTextField(state.customDate, { vm.custom(it, state.customTime) }, Modifier.fillMaxWidth(),
                    enabled = !state.busy, label = { Text(stringResource(R.string.reminder_date)) }, singleLine = true)
                ScrollableTimeField(R.string.reminder_time, state.customTime, !state.busy) {
                    vm.custom(state.customDate, it) }
                Button({ vm.snooze(null) }, enabled = !state.busy) { Text(stringResource(R.string.reminder_custom)) }
            }
        }, confirmButton = { TextButton(vm::dismissSnooze, enabled = !state.busy) {
            Text(stringResource(R.string.cancel)) } })
}

/** Reminders of one task or habit: rows to edit or delete, a new reminder and one-tap shortcuts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListSheet(state: ReminderUiState, actions: ReminderListActions) {
    ModalBottomSheet(onDismissRequest = actions.close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        ReminderListContent(state, actions)
    }
}

@Composable
internal fun ReminderListContent(state: ReminderUiState, actions: ReminderListActions) {
    val enabled = !state.busy && !state.loading
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SheetHeader(stringResource(R.string.reminders), state.title)
        ReminderErrors(state, actions.retry)
        if (state.showsScope) ScopeSelector(state.scopeFuture, enabled, actions.scope)
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Column {
                if (state.definitions.isEmpty() && !state.loading) Text(stringResource(R.string.reminder_empty),
                    Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.definitions.forEach { definition ->
                    ReminderRow(definition, state.target, enabled, onEdit = { actions.edit(definition) },
                        onDelete = { actions.remove(definition) })
                    HorizontalDivider()
                }
                NewReminderRow(enabled) { actions.edit(null) }
            }
        }
        QuickReminders(state.quickOffsets, enabled, actions.quickAdd)
        Text(stringResource(R.string.reminder_deadline_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String, onDelete: (() -> Unit)? = null, enabled: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onDelete != null) {
            val description = stringResource(R.string.reminder_delete)
            IconButton(onDelete, enabled = enabled,
                modifier = Modifier.semantics { contentDescription = description }) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
            }
        }
    }
}

@Composable
private fun ScopeSelector(future: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    SegmentedChoice(stringResource(R.string.reminder_scope), listOf(false, true), future, enabled, change,
        text = { stringResource(if (it) R.string.reminder_scope_future else R.string.reminder_scope_only_this) })
}

@Composable
private fun <T> SegmentedChoice(
    label: String, options: List<T>, selected: T, enabled: Boolean, onSelect: (T) -> Unit,
    text: @Composable (T) -> String, isEnabled: (T) -> Boolean = { true },
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(selected = option == selected, onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    enabled = enabled && isEnabled(option)) {
                    Text(text(option), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    definition: ReminderDefinition, target: ReminderTarget, enabled: Boolean, onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val headline = reminderHeadline(definition, target.today)
    val editLabel = stringResource(R.string.reminder_edit_named, headline)
    val deleteLabel = stringResource(R.string.reminder_delete_named, headline)
    Row(Modifier.fillMaxWidth().clickable(enabled, onClickLabel = editLabel, role = Role.Button, onClick = onEdit)
        .sizeIn(minHeight = 64.dp).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ModeBadge(modeIcon(definition.mode))
        Column(Modifier.weight(1f)) {
            Text(headline, style = MaterialTheme.typography.bodyLarge)
            Text(reminderSupporting(definition, target), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onDelete, enabled = enabled, modifier = Modifier.semantics { contentDescription = deleteLabel }) {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
        }
    }
}

@Composable
private fun NewReminderRow(enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled, role = Role.Button, onClick = onClick).sizeIn(minHeight = 56.dp)
        .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary)
        }
        Text(stringResource(R.string.reminder_new), color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ModeBadge(@DrawableRes icon: Int) {
    Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@DrawableRes
private fun modeIcon(mode: ReminderMode): Int = when (mode) {
    ReminderMode.RELATIVE -> R.drawable.ic_schedule
    ReminderMode.CHOSEN -> R.drawable.ic_event
    ReminderMode.WEEKDAYS -> R.drawable.ic_repeat
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickReminders(offsets: List<Long>, enabled: Boolean, add: (Long) -> Unit) {
    if (offsets.isEmpty()) return
    Text(stringResource(R.string.reminder_quick), style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        offsets.forEach { offset ->
            AssistChip({ add(offset) }, label = { Text(offsetLabel(offset)) }, enabled = enabled,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
                leadingIcon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null,
                    Modifier.size(AssistChipDefaults.IconSize)) })
        }
    }
}

/** Editor sheet (spec 029 style): live summary of when it rings, type, then that type's choices. */
@Composable
fun ReminderEditor(state: ReminderUiState, actions: ReminderEditorActions) {
    val draft = state.draft ?: return
    val title = stringResource(if (draft.editing) R.string.reminder_edit else R.string.reminder_new)
    val enabled = !state.busy
    GrowingSheet(expanded = false, title = title, busy = state.busy, onDismiss = { actions.change(null) },
        onCollapse = null, onSave = actions.save,
        name = { SheetHeader(title, state.title, actions.delete.takeIf { draft.editing }, enabled) }) {
        val definition = draft.asDefinition()
        ReminderSummary(state.target.moment(definition), definition.mode, state.target.today)
        ReminderErrors(state, actions.retry)
        if (definition.kind == ReminderKind.TASK) SegmentedChoice(stringResource(R.string.reminder_mode),
            ReminderMode.entries, definition.mode, enabled,
            onSelect = { actions.change(draft.copy(definition = draft.definition.copy(mode = it))) },
            text = { stringResource(modeLabel(it)) },
            isEnabled = { it != ReminderMode.RELATIVE || state.target.hasDeadline })
        when (definition.mode) {
            ReminderMode.RELATIVE -> RelativeFields(draft, state.target.dueMinute == null, enabled, actions.change)
            ReminderMode.CHOSEN -> ChosenFields(draft, state.target.today, enabled, actions.change)
            ReminderMode.WEEKDAYS -> WeekdayFields(draft, enabled, actions.change)
        }
        Text(stringResource(R.string.reminder_deadline_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun modeLabel(mode: ReminderMode) = when (mode) {
    ReminderMode.RELATIVE -> R.string.reminder_mode_relative
    ReminderMode.CHOSEN -> R.string.reminder_mode_chosen
    ReminderMode.WEEKDAYS -> R.string.reminder_mode_weekdays
}

@Composable
private fun ReminderSummary(moment: ReminderMoment, mode: ReminderMode, today: LocalDate) {
    val warns = !moment.canBeSaved(mode) || (moment is ReminderMoment.At && moment.past)
    val label = when {
        moment is ReminderMoment.NeedsDeadline -> stringResource(R.string.reminder_needs_deadline)
        moment is ReminderMoment.NeedsReference -> stringResource(R.string.reminder_no_reference_time)
        warns -> stringResource(R.string.reminder_past)
        else -> stringResource(R.string.reminder_rings)
    }
    val value = when (moment) {
        is ReminderMoment.At -> stringResource(R.string.reminder_rings_value, summaryDate(moment.dateTime, today),
            TabitosDates.time(moment.dateTime.minuteOfDay()))
        is ReminderMoment.Weekly -> stringResource(R.string.reminder_rings_value, weekdaysLabel(moment.weekdays),
            TabitosDates.time(moment.minute))
        ReminderMoment.NeedsDeadline -> stringResource(R.string.reminder_needs_deadline_hint)
        ReminderMoment.NeedsReference -> ""
    }
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, shape = RoundedCornerShape(16.dp),
        color = if (warns) colors.tertiaryContainer else colors.primaryContainer,
        contentColor = if (warns) colors.onTertiaryContainer else colors.onPrimaryContainer) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.ic_notification_reminder), contentDescription = null)
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium)
                if (value.isNotBlank()) Text(value, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** "Hoje, 19 nov" next to today and tomorrow, otherwise "sáb, 21 nov". */
@Composable
private fun summaryDate(dateTime: LocalDateTime, today: LocalDate): String {
    val date = dateTime.toLocalDate()
    val relative = TabitosDates.short(date, today)
    return if (relative is DateText.Plain) TabitosDates.shortWithWeekday(date, today)
        else stringResource(R.string.reminder_date_time, relative.label(), TabitosDates.shortPlain(date, today))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelativeFields(
    draft: ReminderDraft, needsReference: Boolean, enabled: Boolean, change: (ReminderDraft) -> Unit,
) {
    FieldLabel(R.string.reminder_how_early)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        offsetChoices(draft.offsetMinutes).forEach { offset ->
            ChoiceChip(offsetLabel(offset), draft.offsetMinutes == offset, enabled) {
                change(draft.copy(offsetMinutes = offset)) }
        }
    }
    if (!needsReference) return
    ScrollableTimeField(R.string.reminder_reference_time,
        TabitosDates.time(draft.referenceMinute ?: DEFAULT_REMINDER_MINUTE), enabled) { text ->
        text.toMinuteOfDay()?.let { change(draft.copy(referenceMinute = it)) } }
    Text(stringResource(R.string.reminder_reference_hint), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChosenFields(draft: ReminderDraft, today: LocalDate, enabled: Boolean, change: (ReminderDraft) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val tomorrow = today.plusDays(1)
    val other = draft.date != today && draft.date != tomorrow
    FieldLabel(R.string.reminder_day)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ChoiceChip(stringResource(R.string.today), draft.date == today, enabled) { change(draft.copy(date = today)) }
        ChoiceChip(stringResource(R.string.tomorrow), draft.date == tomorrow, enabled) {
            change(draft.copy(date = tomorrow)) }
        ChoiceChip(if (other) TabitosDates.shortWithWeekday(draft.date, today)
            else stringResource(R.string.reminder_pick_date), other, enabled, R.drawable.ic_calendar_month) {
            picking = true }
    }
    ScrollableTimeField(R.string.reminder_time, TabitosDates.time(draft.minute), enabled) { text ->
        text.toMinuteOfDay()?.let { change(draft.copy(minute = it)) } }
    if (picking) TabitosDatePickerDialog(initial = draft.date, min = today,
        onConfirm = { change(draft.copy(date = it)); picking = false }, onDismiss = { picking = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayFields(draft: ReminderDraft, enabled: Boolean, change: (ReminderDraft) -> Unit) {
    val weekdays = draft.definition.weekdays
    val names = stringArrayResource(R.array.reminder_weekday_names)
    val initials = stringArrayResource(R.array.reminder_weekday_initials)
    val setWeekdays = { mask: Int -> change(draft.copy(definition = draft.definition.copy(weekdays = mask))) }
    FieldLabel(R.string.reminder_days)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (0 until DAYS_IN_WEEK).forEach { index ->
            WeekdayToggle(initials[index], names[index], weekdays.hasWeekday(index), enabled) {
                setWeekdays(weekdays.toggleWeekdayKeepingOne(index)) }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WeekdayPreset.entries.forEach { preset ->
            ChoiceChip(presetLabel(preset), weekdays == preset.mask, enabled) { setWeekdays(preset.mask) }
        }
    }
    ScrollableTimeField(R.string.reminder_time, TabitosDates.time(draft.minute), enabled) { text ->
        text.toMinuteOfDay()?.let { change(draft.copy(minute = it)) } }
}

@Composable
private fun WeekdayToggle(initial: String, name: String, selected: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(44.dp).background(if (selected) colors.primary else Color.Transparent, CircleShape)
        .border(1.dp, if (selected) colors.primary else colors.outline, CircleShape)
        .toggleable(selected, enabled, role = Role.Checkbox) { onToggle() }
        .semantics { contentDescription = name }, contentAlignment = Alignment.Center) {
        Text(initial, style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.onSurface)
    }
}

@Composable
private fun ChoiceChip(
    text: String, selected: Boolean, enabled: Boolean, @DrawableRes icon: Int? = null, onClick: () -> Unit,
) {
    FilterChip(selected, onClick, label = { Text(text) }, enabled = enabled,
        modifier = Modifier.sizeIn(minHeight = 48.dp),
        leadingIcon = if (icon == null) null else {
            { Icon(painterResource(icon), contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
        })
}

@Composable
private fun FieldLabel(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun reminderHeadline(definition: ReminderDefinition, today: LocalDate): String = when (definition.mode) {
    ReminderMode.RELATIVE -> offsetLabel(definition.offsetMinutes)
    ReminderMode.WEEKDAYS -> TabitosDates.time(definition.minute)
    ReminderMode.CHOSEN -> definition.chosenDate?.let {
        stringResource(R.string.reminder_date_time, TabitosDates.short(it, today).label(),
            TabitosDates.time(definition.minute))
    } ?: TabitosDates.time(definition.minute)
}

@Composable
internal fun reminderSupporting(definition: ReminderDefinition, target: ReminderTarget): String =
    when (definition.mode) {
        ReminderMode.WEEKDAYS -> weekdaysLabel(definition.weekdays)
        ReminderMode.CHOSEN -> stringResource(R.string.reminder_chosen_date)
        ReminderMode.RELATIVE -> when (val moment = target.moment(definition)) {
            is ReminderMoment.At -> stringResource(R.string.reminder_rings_at,
                TabitosDates.short(moment.dateTime.toLocalDate(), target.today).label()
                    .replaceFirstChar { it.lowercase() }, TabitosDates.time(moment.dateTime.minuteOfDay()))
            is ReminderMoment.Weekly -> weekdaysLabel(moment.weekdays)
            ReminderMoment.NeedsReference -> stringResource(R.string.reminder_no_reference_time)
            ReminderMoment.NeedsDeadline -> stringResource(R.string.reminder_needs_deadline)
        }
    }

@Composable
internal fun offsetLabel(minutes: Long): String = when (val text = offsetText(minutes)) {
    OffsetText.AtDeadline -> stringResource(R.string.reminder_at_deadline)
    is OffsetText.Days -> pluralStringResource(R.plurals.reminder_days_before, text.count, text.count)
    is OffsetText.Hours -> pluralStringResource(R.plurals.reminder_hours_before, text.count, text.count)
    is OffsetText.Minutes -> pluralStringResource(R.plurals.reminder_minutes_before, text.count, text.count)
}

@Composable
private fun weekdaysLabel(mask: Int): String {
    weekdayPreset(mask)?.let { return presetLabel(it) }
    val short = stringArrayResource(R.array.reminder_weekday_short)
    return weekdayIndexes(mask).joinToString(", ") { short[it] }.ifEmpty { stringResource(R.string.reminder_no_days) }
}

@Composable
private fun presetLabel(preset: WeekdayPreset): String = stringResource(when (preset) {
    WeekdayPreset.EVERY_DAY -> R.string.reminder_every_day
    WeekdayPreset.MONDAY_TO_FRIDAY -> R.string.reminder_monday_to_friday
    WeekdayPreset.WEEKEND -> R.string.reminder_weekend
})

private fun LocalDateTime.minuteOfDay(): Int = hour * MINUTES_PER_HOUR_INT + minute
private fun String.toMinuteOfDay(): Int? =
    runCatching { LocalTime.parse(this) }.getOrNull()?.let { it.hour * MINUTES_PER_HOUR_INT + it.minute }

private const val MINUTES_PER_HOUR_INT = 60
private val SNOOZE_MINUTES = listOf(10, 30, 60)

@Composable
internal fun ReminderErrors(state: ReminderUiState, retry: () -> Unit) {
    if (state.loading) Text(stringResource(R.string.reminder_loading))
    if (state.error) {
        Text(stringResource(R.string.reminder_storage_error), color = MaterialTheme.colorScheme.error)
        TextButton(retry) { Text(stringResource(R.string.retry)) }
    }
    if (state.inputError) Text(stringResource(R.string.reminder_input_error), color = MaterialTheme.colorScheme.error)
}

@Preview
@Composable
private fun ReminderListPreview() {
    val now = LocalDateTime.of(2026, 11, 19, 9, 41)
    val relative = ReminderDefinition("r1", kind = ReminderKind.TASK, sourceId = "t", mode = ReminderMode.RELATIVE,
        offsetMinutes = 30)
    val chosen = relative.copy(id = "r2", groupId = "r2", mode = ReminderMode.CHOSEN,
        chosenDate = LocalDate.of(2026, 11, 18), minute = 20 * 60, offsetMinutes = 0)
    TabitosTheme {
        Surface {
            ReminderListContent(ReminderUiState(loading = false, kind = ReminderKind.TASK, targetId = "t:1",
                title = "Pagar conta de luz", definitions = listOf(relative, chosen),
                target = ReminderTarget(LocalDate.of(2026, 11, 19), 18 * 60, now), repeats = true),
                ReminderListActions(close = {}, scope = {}, edit = {}, remove = {}, quickAdd = {}, retry = {}))
        }
    }
}
