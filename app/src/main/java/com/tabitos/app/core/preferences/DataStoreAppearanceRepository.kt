package com.tabitos.app.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class DataStoreAppearanceRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : AppearanceRepository {
    override val modes = store.data.map { preferences ->
        AppearanceMode.entries.firstOrNull { it.name == preferences[MODE_KEY] } ?: AppearanceMode.SYSTEM
    }

    override suspend fun setMode(mode: AppearanceMode) {
        store.edit { it[MODE_KEY] = mode.name }
    }

    override val timerAlarm = store.data.map { it[TIMER_ALARM_KEY] ?: true }

    override suspend fun setTimerAlarm(enabled: Boolean) {
        store.edit { it[TIMER_ALARM_KEY] = enabled }
    }

    companion object {
        val MODE_KEY = stringPreferencesKey("appearance_mode")
        val TIMER_ALARM_KEY = booleanPreferencesKey("timer_alarm")
    }
}
