package com.tabitos.app.core.backup

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.withTransaction
import com.tabitos.app.core.reminders.platform.ReminderCoordinator
import com.tabitos.app.core.tasks.data.TaskDatabase
import com.tabitos.app.core.tasks.domain.TaskClock
import com.tabitos.app.core.timers.domain.TimerClock
import com.tabitos.app.core.timers.domain.now
import com.tabitos.app.core.timers.platform.TimerCoordinator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class RoomBackupRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: TaskDatabase,
    private val preferences: DataStore<Preferences>,
    private val clock: TaskClock,
    private val timerClock: TimerClock,
    private val journal: BackupJournal,
    private val reminders: ReminderCoordinator,
    private val timers: TimerCoordinator,
) : BackupRepository {
    /** Export, inspection and restore never interleave, so a preview always reflects one destination state. */
    private val lock = Mutex()

    override suspend fun export(document: String, password: CharArray?) = lock.withLock {
        withContext(Dispatchers.IO) {
            val stored = preferences.data.first()
            val captured = database.withTransaction {
                val rows = RoomBackupRows.capture(database.openHelper.writableDatabase, timerClock.now())
                BackupDocument(clock.nowMillis(), rows, BackupPreferences.encode(stored))
            }
            // "wt" truncates a file the picker reused, so a shorter backup never keeps a stale tail.
            val output = context.contentResolver.openOutputStream(document.toUri(), "wt")
                ?: throw FileNotFoundException("Destination unavailable")
            output.use { BackupArchive.write(it, captured.encode(), password) }
        }
    }

    override suspend fun inspect(document: String, password: CharArray?): BackupInspection = lock.withLock {
        withContext(Dispatchers.IO) {
            val payload = try {
                read(document, password)
            } catch (_: BackupPasswordRequired) {
                return@withContext BackupInspection.PasswordRequired
            }
            val content = BackupDocument.decode(payload)
            BackupPreferences.decode(content.preferences)
            val storedPreferences = preferences.data.first()
            database.withTransaction {
                val db = database.openHelper.writableDatabase
                val counts = RoomBackupRows.inspect(content.rows, db)
                val personal = RoomBackupRows.hasPersonalData(db) || storedPreferences.asMap().isNotEmpty()
                BackupInspection.Ready(
                    BackupPreview(
                        capturedAt = content.capturedAt,
                        lists = counts.lists,
                        tasks = counts.tasks,
                        habits = counts.habits,
                        hasPersonalData = personal,
                        rows = content.rows,
                        preferences = content.preferences,
                    ),
                )
            }
        }
    }

    override suspend fun restore(preview: BackupPreview, confirmedReplacement: Boolean) = lock.withLock {
        withContext(Dispatchers.IO) {
            val applyPreferences = BackupPreferences.decode(preview.preferences)
            val storedPreferences = preferences.data.first()
            database.withTransaction {
                val db = database.openHelper.writableDatabase
                val personal = RoomBackupRows.hasPersonalData(db) || storedPreferences.asMap().isNotEmpty()
                if (personal && !confirmedReplacement) throw ReplacementRequired()
                RoomBackupRows.replace(db, preview.rows)
                RoomBackupRows.retireOverdueReminders(db, clock.nowMillis())
                // Written before the commit: if the process dies between the commit and the DataStore write,
                // the next start finds the journal, sees the restored rows and finishes the preferences.
                journal.write(RoomBackupRows.fingerprint(db, timerClock.now()), preview.preferences)
            }
            preferences.updateData { current -> current.toMutablePreferences().also(applyPreferences) }
            journal.clear()
        }
        reminders.request()
        timers.request()
    }

    private fun read(document: String, password: CharArray?): ByteArray {
        val input = context.contentResolver.openInputStream(document.toUri())
            ?: throw FileNotFoundException("Backup unavailable")
        return input.use { BackupArchive.read(it, password) }
    }

    /**
     * Finishes a restore interrupted after its database commit. A journal whose fingerprint does not match the
     * rows belongs to a restore that never committed, so the current routine and preferences stay as they are.
     */
    suspend fun recoverInterruptedRestore() = lock.withLock {
        withContext(Dispatchers.IO) {
            val pending = journal.read() ?: return@withContext
            val committed = database.withTransaction {
                RoomBackupRows.fingerprint(database.openHelper.writableDatabase, timerClock.now())
                    .contentEquals(pending.fingerprint)
            }
            if (committed) {
                // The journal is private to this installation; an unreadable one cannot be finished safely.
                val applyPreferences = runCatching { BackupPreferences.decode(pending.preferences) }.getOrNull()
                applyPreferences?.let { apply ->
                    preferences.updateData { current -> current.toMutablePreferences().also(apply) }
                }
            }
            journal.clear()
        }
    }
}
