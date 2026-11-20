package com.tabitos.app.core.tasks.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun observe(): Flow<TaskSnapshot>
    suspend fun execute(action: TaskAction)

    /** Returns whether this creation configured reminders that may require notification permission. */
    suspend fun createWithReminders(action: TaskAction.Create): Boolean {
        execute(action)
        return false
    }
}

interface TaskClock {
    fun today(): LocalDate
    fun nowMillis(): Long
}
