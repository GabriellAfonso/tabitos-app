package com.ethos.app.core.demo.data

import android.content.Context
import androidx.room.withTransaction
import com.ethos.app.BuildConfig
import com.ethos.app.R
import com.ethos.app.core.demo.domain.DemoRepository
import com.ethos.app.core.demo.domain.DemoTimerRunningException
import com.ethos.app.core.reminders.platform.ReminderCoordinator
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.timers.domain.TimerClock
import com.ethos.app.core.timers.domain.TimerState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RoomDemoRepository @Inject constructor(
    private val db: TaskDatabase,
    private val clock: TaskClock,
    private val timerClock: TimerClock,
    private val reminders: ReminderCoordinator,
    @param:ApplicationContext private val context: Context,
) : DemoRepository {
    override suspend fun restore() {
        check(BuildConfig.DEBUG)
        reminders.replaceLocalData {
            restoreContent(db, clock, timerClock, context.resources.getStringArray(R.array.demo_content))
        }
    }

    companion object {
        internal suspend fun restoreContent(
            db: TaskDatabase, clock: TaskClock, timerClock: TimerClock, text: Array<String>,
        ) = db.withTransaction {
            if (db.timers().sessions().any { it.domain().state == TimerState.RUNNING }) {
                throw DemoTimerRunningException()
            }
            clearContent(db)
            DemoSeed(db, clock, timerClock, text).populate()
        }
        /** Room schema stays intact; only user-content rows participate in this explicit reset. */
        internal fun clearContent(db: TaskDatabase) {
            val sql = db.openHelper.writableDatabase
            sql.execSQL("PRAGMA defer_foreign_keys = ON")
            val tables = sql.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(0)
                        if (!name.startsWith("sqlite_") && name !in setOf("room_master_table", "android_metadata")) {
                            add(name)
                        }
                    }
                }
            }
            tables.forEach { sql.execSQL("DELETE FROM \"${it.replace("\"", "\"\"")}\"") }
        }
    }
}
