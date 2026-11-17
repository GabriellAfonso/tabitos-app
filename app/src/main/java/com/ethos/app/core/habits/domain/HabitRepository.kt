package com.ethos.app.core.habits.domain

import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observe(): Flow<HabitSnapshot>
    suspend fun execute(action: HabitAction)
    /** Deletes the habit completely with its history; coins and XP already earned stay (spec 022). */
    suspend fun erase(habitId: String)
}
