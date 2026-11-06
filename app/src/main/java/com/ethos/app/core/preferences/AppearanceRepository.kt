package com.ethos.app.core.preferences

import kotlinx.coroutines.flow.Flow

enum class AppearanceMode { SYSTEM, LIGHT, DARK }

interface AppearanceRepository {
    val modes: Flow<AppearanceMode>
    suspend fun setMode(mode: AppearanceMode)
}
