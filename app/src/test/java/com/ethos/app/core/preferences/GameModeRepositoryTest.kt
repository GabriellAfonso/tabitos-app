package com.ethos.app.core.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GameModeRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun gameModeIsOnByDefaultAndSurvivesNewStoreWithoutTouchingTheme() = runBlocking {
        val file = File(temporary.root, "appearance.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val firstStore = PreferenceDataStoreFactory.create(scope = firstScope) { file }
        val first = DataStoreGameModeRepository(firstStore)
        DataStoreAppearanceRepository(firstStore).setMode(AppearanceMode.DARK)
        assertTrue(first.enabled.first())
        first.setEnabled(false)
        firstScope.cancel()
        firstScope.coroutineContext[Job]!!.join()
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = secondScope) { file }
            assertFalse(DataStoreGameModeRepository(store).enabled.first())
            assertEquals(AppearanceMode.DARK, DataStoreAppearanceRepository(store).modes.first())
        } finally {
            secondScope.cancel()
            secondScope.coroutineContext[Job]!!.join()
        }
    }
}
