package com.ethos.app.core.tasks.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun observe(): Flow<TaskSnapshot>
    suspend fun execute(action: TaskAction)
}

interface TaskClock {
    fun today(): LocalDate
    fun nowMillis(): Long
}
