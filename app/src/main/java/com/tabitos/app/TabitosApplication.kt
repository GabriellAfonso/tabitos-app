package com.tabitos.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.tabitos.app.core.backup.BackupJournal
import com.tabitos.app.core.backup.RoomBackupRepository
import com.tabitos.app.core.reminders.platform.ReminderCoordinator
import com.tabitos.app.core.timers.platform.TimerCoordinator
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class TabitosApplication : Application() {
    @Inject lateinit var reminders: ReminderCoordinator
    @Inject lateinit var timers: TimerCoordinator
    @Inject lateinit var backupJournal: BackupJournal
    @Inject lateinit var backups: RoomBackupRepository

    override fun onCreate() {
        super.onCreate()
        finishInterruptedRestore()
        reminders.start()
        timers.start()
    }

    // Blocks only in the rare start after a restore was cut short, so observers never see half-applied data.
    private fun finishInterruptedRestore() {
        if (backupJournal.exists()) runBlocking { backups.recoverInterruptedRestore() }
    }
}
