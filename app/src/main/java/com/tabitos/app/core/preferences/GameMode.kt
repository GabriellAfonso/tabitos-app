package com.tabitos.app.core.preferences

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

    companion object {
        /** New installs start without coins, XP and rewards; the user opts in from Settings. */
        const val DEFAULT_ENABLED = false
    }
}

interface GameModeRepository : GameModeSource {
    suspend fun setEnabled(enabled: Boolean)
}

class DataStoreGameModeRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : GameModeRepository {
    override val enabled = store.data.map { it[GAME_MODE_KEY] ?: GameModeSource.DEFAULT_ENABLED }

    override suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[GAME_MODE_KEY] = enabled }
    }

    companion object {
        val GAME_MODE_KEY = booleanPreferencesKey("game_mode")
    }
}

/** Mode that ignores the stored preference; tests use it to choose whether outcomes are scored. */
class FixedGameMode(on: Boolean) : GameModeSource {
    override val enabled: Flow<Boolean> = flowOf(on)
}
