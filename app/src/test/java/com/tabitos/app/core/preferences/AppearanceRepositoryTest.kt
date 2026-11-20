package com.tabitos.app.core.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppearanceRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun persistedModeSurvivesNewStoreInstance() = runBlocking {
        val file = File(temporary.root, "appearance.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val first = DataStoreAppearanceRepository(PreferenceDataStoreFactory.create(scope = firstScope) { file })
        assertEquals(AppearanceMode.SYSTEM, first.modes.first())
        first.setMode(AppearanceMode.DARK)
        firstScope.cancel()
        firstScope.coroutineContext[Job]!!.join()
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val second = DataStoreAppearanceRepository(PreferenceDataStoreFactory.create(scope = secondScope) { file })
            assertEquals(AppearanceMode.DARK, second.modes.first())
        } finally {
            secondScope.cancel()
            secondScope.coroutineContext[Job]!!.join()
        }
    }

    @Test fun unknownFutureValueUsesSystemWithoutOverwritingStorage() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) {
                File(temporary.root, "unknown.preferences_pb")
            }
            store.edit { it[DataStoreAppearanceRepository.MODE_KEY] = "FUTURE_MODE" }
            val repository = DataStoreAppearanceRepository(store)
            assertEquals(AppearanceMode.SYSTEM, repository.modes.first())
            assertEquals("FUTURE_MODE", store.data.first()[DataStoreAppearanceRepository.MODE_KEY])
        } finally {
            scope.cancel()
            scope.coroutineContext[Job]!!.join()
        }
    }
}
