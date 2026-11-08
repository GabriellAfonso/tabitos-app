package com.ethos.app.core.reminders.data

import androidx.room.withTransaction
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.reminders.domain.*
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReminderRepository @Inject constructor(private val db: TaskDatabase) : ReminderRepository {
    override fun observe() = db.invalidationTracker.createFlow(
        "reminder_definitions", "reminder_deliveries", "reminder_settings", emitInitialState = true,
    ).map { read() }

    override suspend fun read(): ReminderSnapshot = db.withTransaction {
        val dao = db.reminders()
        val settings = dao.settings()
        ReminderSnapshot(dao.definitions().map { it.domain() }, dao.deliveries().map { it.domain() },
            ReminderSettings(settings?.summaryMinute, settings?.lastSummaryDate?.let(LocalDate::parse)))
    }
    override suspend fun save(definition: ReminderDefinition) {
        ReminderRules.validate(definition)
        db.withTransaction {
            if (definition.kind == ReminderKind.TASK && definition.occurrenceId == null) {
                // This-and-future replaces exceptions from this ordinal, preserving earlier scopes.
                db.reminders().definitions().map { it.domain() }.filter {
                    it.groupId == definition.groupId && it.fromOrdinal >= definition.fromOrdinal &&
                        it.id != definition.id
                }.forEach { db.reminders().remove(it.id) }
            }
            db.reminders().definition(definition.entity())
        }
    }
    override suspend fun remove(definition: ReminderDefinition) {
        // Disabled scoped rows suppress inherited series rules without deleting the series definition.
        save(definition.copy(enabled = false))
    }
    override suspend fun deliveries(rows: List<ReminderDelivery>) {
        if (rows.isNotEmpty()) db.withTransaction { db.reminders().deliveries(rows.map { it.entity() }) }
    }
    override suspend fun summary(minute: Int?) {
        require(minute == null || minute in 0..1439)
        db.withTransaction {
            val old = db.reminders().settings() ?: ReminderSettingsEntity()
            val time = java.time.LocalTime.now()
            val alreadyPast = minute != null && minute <= time.hour * 60 + time.minute
            db.reminders().settings(old.copy(summaryMinute = minute,
                lastSummaryDate = if (alreadyPast) LocalDate.now().toString() else old.lastSummaryDate))
        }
    }
    override suspend fun summaryDelivered(date: LocalDate) {
        db.withTransaction {
            val old = db.reminders().settings() ?: ReminderSettingsEntity()
            db.reminders().settings(old.copy(lastSummaryDate = date.toString()))
        }
    }
    override suspend fun snooze(key: String, instant: Long, now: Long) {
        db.withTransaction {
            val row = db.reminders().deliveries().firstOrNull { it.key == key }?.domain()
                ?: error("Missing reminder")
            db.reminders().deliveries(listOf(ReminderRules.snooze(row, instant, now).entity()))
        }
    }
}
