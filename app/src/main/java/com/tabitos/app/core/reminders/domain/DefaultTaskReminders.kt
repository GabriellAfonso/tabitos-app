package com.tabitos.app.core.reminders.domain

import com.tabitos.app.core.preferences.TaskReminderDefaults
import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.RepeatKind
import com.tabitos.app.core.tasks.domain.TaskAction
import java.time.Duration
import java.time.ZoneId

fun defaultTaskReminders(
    action: TaskAction.Create,
    defaults: TaskReminderDefaults,
    createdAt: Long,
    zone: ZoneId,
): List<ReminderDefinition> {
    val fields = action.fields
    val date = fields.date ?: return emptyList()
    if (!fields.deadline.hasDayTime) return emptyList()
    val minute = fields.minute ?: defaults.dateOnlyMinute ?: return emptyList()
    val offsets = if (fields.minute == null) setOf(0) else defaults.offsetMinutes
    val deadline = ReminderRules.instant(date, minute, zone)
    return offsets.mapNotNull { offset ->
        // A series keeps its rule even when the first occurrence is too late for this offset.
        val scheduledAt = deadline - Duration.ofMinutes(offset.toLong()).toMillis()
        if (action.rule.kind == RepeatKind.NONE && scheduledAt < createdAt) return@mapNotNull null
        val group = "default:${action.id}:$offset"
        ReminderDefinition(
            id = "$group:from:1",
            groupId = group,
            kind = ReminderKind.TASK,
            sourceId = action.id,
            mode = ReminderMode.RELATIVE,
            offsetMinutes = offset.toLong(),
            referenceMinute = minute.takeIf { fields.minute == null },
            createdAt = createdAt,
        )
    }
}
