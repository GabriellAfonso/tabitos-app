package com.tabitos.app.features.reminders

import com.tabitos.app.core.reminders.domain.ReminderDefinition
import com.tabitos.app.core.reminders.domain.ReminderMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Default time of a new reminder and of the reference for tasks due on a day without a time. */
const val DEFAULT_REMINDER_MINUTE = 9 * 60
const val DEFAULT_REMINDER_OFFSET = 30L
private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

/** Shortcuts of the reminders sheet: at the deadline, 30 min, 1 h and 1 day before. */
val QUICK_REMINDER_OFFSETS = listOf(0L, DEFAULT_REMINDER_OFFSET, MINUTES_PER_HOUR, MINUTES_PER_DAY)

/** Offsets offered by the editor; a saved offset outside this list is added so it stays selectable. */
private val OFFSET_CHOICES = listOf(0L, 5L, 10L, 15L, DEFAULT_REMINDER_OFFSET, MINUTES_PER_HOUR,
    2 * MINUTES_PER_HOUR, MINUTES_PER_DAY, 2 * MINUTES_PER_DAY)

/** What the open task or habit gives the reminder texts: its deadline and the current moment. */
data class ReminderTarget(
    val dueDate: LocalDate? = null,
    val dueMinute: Int? = null,
    val now: LocalDateTime = LocalDateTime.MIN,
) {
    val today: LocalDate get() = now.toLocalDate()
    val hasDeadline: Boolean get() = dueDate != null
}

/** When a reminder rings, as shown on its row and on the editor summary. */
sealed interface ReminderMoment {
    data class At(val dateTime: LocalDateTime, val past: Boolean) : ReminderMoment
    data class Weekly(val weekdays: Int, val minute: Int) : ReminderMoment
    data object NeedsReference : ReminderMoment
    data object NeedsDeadline : ReminderMoment
}

fun ReminderTarget.moment(definition: ReminderDefinition): ReminderMoment = when (definition.mode) {
    ReminderMode.WEEKDAYS -> ReminderMoment.Weekly(definition.weekdays, definition.minute)
    ReminderMode.CHOSEN -> definition.chosenDate?.let { at(it, definition.minute, 0) } ?: ReminderMoment.NeedsDeadline
    ReminderMode.RELATIVE -> when {
        dueDate == null -> ReminderMoment.NeedsDeadline
        else -> (dueMinute ?: definition.referenceMinute)?.let { at(dueDate, it, definition.offsetMinutes) }
            ?: ReminderMoment.NeedsReference
    }
}

private fun ReminderTarget.at(date: LocalDate, minute: Int, offsetMinutes: Long): ReminderMoment.At {
    val dateTime = date.atTime(LocalTime.of(minute / 60, minute % 60)).minusMinutes(offsetMinutes)
    return ReminderMoment.At(dateTime, past = !dateTime.isAfter(now))
}

/**
 * A chosen date must still be ahead; a relative reminder already past for this occurrence is kept, since it
 * still rings for the next ones of a series and follows the deadline when it is rescheduled.
 */
fun ReminderMoment.canBeSaved(mode: ReminderMode): Boolean = when (this) {
    is ReminderMoment.At -> mode != ReminderMode.CHOSEN || !past
    is ReminderMoment.Weekly -> true
    ReminderMoment.NeedsReference, ReminderMoment.NeedsDeadline -> false
}

/** Shortcuts not yet used by a relative reminder; none without a deadline to count from. */
fun ReminderTarget.quickOffsets(definitions: List<ReminderDefinition>): List<Long> {
    if (!hasDeadline) return emptyList()
    val used = definitions.filter { it.mode == ReminderMode.RELATIVE }.map { it.offsetMinutes }.toSet()
    return QUICK_REMINDER_OFFSETS.filterNot { it in used }
}

fun offsetChoices(current: Long): List<Long> = (OFFSET_CHOICES + current).distinct().sorted()

/** How an offset is read: "na hora", whole days, whole hours or minutes. */
sealed interface OffsetText {
    data object AtDeadline : OffsetText
    data class Days(val count: Int) : OffsetText
    data class Hours(val count: Int) : OffsetText
    data class Minutes(val count: Int) : OffsetText
}

fun offsetText(minutes: Long): OffsetText = when {
    minutes == 0L -> OffsetText.AtDeadline
    minutes % MINUTES_PER_DAY == 0L -> OffsetText.Days((minutes / MINUTES_PER_DAY).toInt())
    minutes % MINUTES_PER_HOUR == 0L -> OffsetText.Hours((minutes / MINUTES_PER_HOUR).toInt())
    else -> OffsetText.Minutes(minutes.toInt())
}

/** Weekday masks use bit 0 for Monday, as [com.tabitos.app.core.reminders.domain.ReminderRules] does. */
enum class WeekdayPreset(val mask: Int) { EVERY_DAY(0b1111111), MONDAY_TO_FRIDAY(0b0011111), WEEKEND(0b1100000) }

fun weekdayPreset(mask: Int): WeekdayPreset? = WeekdayPreset.entries.firstOrNull { it.mask == mask }
fun weekdayIndexes(mask: Int): List<Int> = (0 until DAYS_IN_WEEK).filter { mask and (1 shl it) != 0 }
fun Int.hasWeekday(index: Int): Boolean = this and (1 shl index) != 0
fun Int.toggleWeekday(index: Int): Int = this xor (1 shl index)

/** A weekday reminder needs at least one day, so the last selected day cannot be turned off. */
fun Int.toggleWeekdayKeepingOne(index: Int): Int = toggleWeekday(index).takeIf { it != 0 } ?: this
const val DAYS_IN_WEEK = 7
