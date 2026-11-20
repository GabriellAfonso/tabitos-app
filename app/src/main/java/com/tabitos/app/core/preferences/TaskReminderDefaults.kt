package com.tabitos.app.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TaskReminderDefaults(
    val offsetMinutes: Set<Int> = setOf(HALF_HOUR),
    val dateOnlyMinute: Int? = null,
) : java.io.Serializable {
    init {
        require(dateOnlyMinute == null || dateOnlyMinute in 0 until ONE_DAY)
        require(offsetMinutes.all { it == HALF_HOUR || it == ONE_DAY })
    }

    val enabled: Boolean get() = offsetMinutes.isNotEmpty() || dateOnlyMinute != null

    companion object {
        const val HALF_HOUR = 30
        const val ONE_DAY = 24 * 60
    }
}

interface TaskReminderDefaultsRepository {
    val defaults: Flow<TaskReminderDefaults>
    suspend fun save(defaults: TaskReminderDefaults)
}

class DataStoreTaskReminderDefaults @Inject constructor(
    private val store: DataStore<Preferences>,
) : TaskReminderDefaultsRepository {
    private val offsetsKey = stringSetPreferencesKey("task_reminder_default_offsets")
    private val dayTimeKey = intPreferencesKey("task_reminder_default_day_time")

    override val defaults = store.data.map { prefs ->
        val offsets = prefs[offsetsKey]?.map(String::toInt)?.toSet() ?: setOf(TaskReminderDefaults.HALF_HOUR)
        TaskReminderDefaults(offsets, prefs[dayTimeKey])
    }

    override suspend fun save(defaults: TaskReminderDefaults) {
        store.edit { prefs ->
            prefs[offsetsKey] = defaults.offsetMinutes.map(Int::toString).toSet()
            if (defaults.dateOnlyMinute == null) prefs.remove(dayTimeKey)
            else prefs[dayTimeKey] = defaults.dateOnlyMinute
        }
    }
}
