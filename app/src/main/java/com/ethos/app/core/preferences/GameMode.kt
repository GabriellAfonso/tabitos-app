package com.ethos.app.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Whether coins, XP, levels and rewards are active (spec 017). Read-only side used by repositories that record
 * outcomes, so scoring is decided in the data layer at write time.
 */
interface GameModeSource {
    val enabled: Flow<Boolean>
}

interface GameModeRepository : GameModeSource {
    suspend fun setEnabled(enabled: Boolean)
}

class DataStoreGameModeRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : GameModeRepository {
    override val enabled = store.data.map { it[GAME_MODE_KEY] ?: true }

    override suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[GAME_MODE_KEY] = enabled }
    }

    companion object {
        val GAME_MODE_KEY = booleanPreferencesKey("game_mode")
    }
}

/** Mode that ignores the preference: the debug demo always seeds a scored wallet and never writes the mode. */
class FixedGameMode(on: Boolean) : GameModeSource {
    override val enabled: Flow<Boolean> = flowOf(on)
}
