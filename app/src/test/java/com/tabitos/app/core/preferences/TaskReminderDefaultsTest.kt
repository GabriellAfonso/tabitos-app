package com.tabitos.app.core.preferences

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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TaskReminderDefaultsTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun disabledOffsetsAndOptionalDayTimeSurviveReopening() = runBlocking {
        val file = File(temporary.root, "appearance.preferences_pb")
        suspend fun withStore(check: suspend (DataStoreTaskReminderDefaults) -> Unit) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val store = PreferenceDataStoreFactory.create(scope = scope) { file }
                check(DataStoreTaskReminderDefaults(store))
            } finally {
                scope.cancel()
                scope.coroutineContext[Job]!!.join()
            }
        }
        val dateOnly = TaskReminderDefaults(emptySet(), 9 * 60)
        withStore { repository ->
            assertEquals(TaskReminderDefaults(setOf(30), null), repository.defaults.first())
            repository.save(dateOnly)
        }
        withStore { repository ->
            assertEquals(dateOnly, repository.defaults.first())
            repository.save(TaskReminderDefaults(setOf(30, 1440), null))
        }
        withStore { assertEquals(TaskReminderDefaults(setOf(30, 1440), null), it.defaults.first()) }
    }
}
