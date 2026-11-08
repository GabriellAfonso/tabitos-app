package com.ethos.app.core.tasks.data

import android.content.Context
import androidx.room.Room
import com.ethos.app.core.accounting.data.*
import com.ethos.app.core.rewards.data.*
import com.ethos.app.core.reminders.data.*
import com.ethos.app.core.reminders.domain.ReminderRepository
import com.ethos.app.core.rewards.domain.RewardRepository
import com.ethos.app.core.accounting.domain.AccountingRepository
import com.ethos.app.core.habits.data.HABIT_MIGRATION_1_2
import com.ethos.app.core.habits.data.RoomHabitRepository
import com.ethos.app.core.habits.domain.HabitRepository
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.tasks.domain.TaskRepository
import com.ethos.app.core.timers.data.RoomTimerRepository
import com.ethos.app.core.timers.data.TIMER_MIGRATION_5_6
import com.ethos.app.core.timers.domain.TimerClock
import com.ethos.app.core.timers.domain.TimerRepository
import com.ethos.app.core.timers.platform.AndroidTimerClock
import com.ethos.app.core.trackers.data.RoomTrackerRepository
import com.ethos.app.core.trackers.data.TRACKER_MIGRATION_6_7
import com.ethos.app.core.trackers.domain.TrackerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TaskModule {
    @Provides @Singleton fun clock(): TaskClock = object : TaskClock {
        override fun today(): LocalDate = LocalDate.now()
        override fun nowMillis(): Long = System.currentTimeMillis()
    }
    @Provides @Singleton fun database(@ApplicationContext context: Context): TaskDatabase =
        Room.databaseBuilder(context, TaskDatabase::class.java, "ethos-tasks.db")
            .addMigrations(HABIT_MIGRATION_1_2, ACCOUNTING_MIGRATION_2_3, REWARD_MIGRATION_3_4,
                REMINDER_MIGRATION_4_5, TIMER_MIGRATION_5_6, TRACKER_MIGRATION_6_7).build()
    @Provides @Singleton fun reminders(repository: RoomReminderRepository): ReminderRepository = repository
    @Provides @Singleton fun timerClock(@ApplicationContext context: Context): TimerClock =
        AndroidTimerClock(context)
    @Provides @Singleton fun timers(database: TaskDatabase, clock: TimerClock): TimerRepository =
        RoomTimerRepository(database, clock)
    @Provides @Singleton
    fun rewards(database: TaskDatabase, clock: TaskClock, timerClock: TimerClock): RewardRepository =
        RoomRewardRepository(database, clock, timerClock)
    @Provides @Singleton fun accounting(database: TaskDatabase, clock: TaskClock): AccountingRepository =
        RoomAccountingRepository(database, clock)
    @Provides @Singleton fun habits(database: TaskDatabase, clock: TaskClock): HabitRepository =
        RoomHabitRepository(database, clock)
    @Provides @Singleton fun trackers(database: TaskDatabase, clock: TaskClock): TrackerRepository =
        RoomTrackerRepository(database, clock)
    @Provides @Singleton fun repository(database: TaskDatabase, clock: TaskClock): TaskRepository =
        RoomTaskRepository(database, clock)
}
