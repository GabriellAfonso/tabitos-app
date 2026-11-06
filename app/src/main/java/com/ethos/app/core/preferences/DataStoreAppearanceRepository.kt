package com.ethos.app.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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

    companion object {
        val MODE_KEY = stringPreferencesKey("appearance_mode")
    }
}
