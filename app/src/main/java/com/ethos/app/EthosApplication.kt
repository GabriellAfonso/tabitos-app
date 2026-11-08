package com.ethos.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.ethos.app.core.reminders.platform.ReminderCoordinator
import javax.inject.Inject

@HiltAndroidApp
class EthosApplication : Application() {
    @Inject lateinit var reminders: ReminderCoordinator
    override fun onCreate() {
        super.onCreate()
        reminders.start()
    }
}
