package com.ethos.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.ethos.app.core.reminders.platform.ReminderCoordinator
import com.ethos.app.core.timers.platform.TimerCoordinator
import javax.inject.Inject

@HiltAndroidApp
class EthosApplication : Application() {
    @Inject lateinit var reminders: ReminderCoordinator
    @Inject lateinit var timers: TimerCoordinator
    override fun onCreate() {
        super.onCreate()
        reminders.start()
        timers.start()
    }
}
