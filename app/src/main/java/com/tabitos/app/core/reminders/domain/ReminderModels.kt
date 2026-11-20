package com.tabitos.app.core.reminders.domain

import com.tabitos.app.core.tasks.domain.TaskSnapshot
import com.tabitos.app.core.habits.domain.HabitSnapshot
import com.tabitos.app.core.trackers.domain.TrackerSnapshot
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class ReminderKind { TASK, HABIT, TRACKER }
enum class ReminderMode { CHOSEN, RELATIVE, WEEKDAYS }
enum class DeliveryState { PENDING, POSTED, RECOVERED, CANCELLED }

data class ReminderDefinition(
    val id: String, val groupId: String = id, val kind: ReminderKind, val sourceId: String,
    val occurrenceId: String? = null, val fromOrdinal: Int = 1, val enabled: Boolean = true,
    val mode: ReminderMode = ReminderMode.CHOSEN, val chosenDate: LocalDate? = null,
    val anchorDay: Long = 0, val minute: Int = 540, val weekdays: Int = 127,
    val offsetMinutes: Long = 0, val referenceMinute: Int? = null, val createdAt: Long = 0,
) : java.io.Serializable

data class ReminderDelivery(
    val key: String, val groupId: String, val kind: ReminderKind, val sourceId: String,
    val periodKey: String, val slotDate: LocalDate, val anchorDate: LocalDate?, val dueAt: Long,
    val snoozed: Boolean = false, val state: DeliveryState = DeliveryState.PENDING,
)

data class ReminderSettings(val summaryMinute: Int? = null, val lastSummaryDate: LocalDate? = null)
data class ReminderSnapshot(
    val definitions: List<ReminderDefinition> = emptyList(),
    val deliveries: List<ReminderDelivery> = emptyList(), val settings: ReminderSettings = ReminderSettings(),
)
data class ReminderSources(
    val tasks: TaskSnapshot, val habits: HabitSnapshot, val trackers: TrackerSnapshot = TrackerSnapshot(),
)

interface ReminderRepository {
    fun observe(): Flow<ReminderSnapshot>
    suspend fun read(): ReminderSnapshot
    suspend fun save(definition: ReminderDefinition)
    suspend fun remove(definition: ReminderDefinition)
    suspend fun deliveries(rows: List<ReminderDelivery>)
    suspend fun summary(minute: Int?)
    suspend fun claimReview(date: LocalDate, minute: Int): Boolean
    suspend fun snooze(key: String, instant: Long, now: Long)
}
