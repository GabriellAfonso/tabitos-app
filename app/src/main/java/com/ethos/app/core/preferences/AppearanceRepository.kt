package com.ethos.app.core.preferences

import kotlinx.coroutines.flow.Flow

enum class AppearanceMode { SYSTEM, LIGHT, DARK }

interface AppearanceRepository {
    val modes: Flow<AppearanceMode>
    suspend fun setMode(mode: AppearanceMode)
    /** Whether a timer reaching its duration rings as an alarm (spec 016); on by default. */
    val timerAlarm: Flow<Boolean>
    suspend fun setTimerAlarm(enabled: Boolean)
}
