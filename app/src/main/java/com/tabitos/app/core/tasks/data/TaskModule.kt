package com.tabitos.app.core.tasks.data

import android.content.Context
import androidx.room.Room
import com.tabitos.app.core.accounting.data.*
import com.tabitos.app.core.rewards.data.*
import com.tabitos.app.core.reminders.data.*
import com.tabitos.app.core.reminders.domain.ReminderRepository
import com.tabitos.app.core.rewards.domain.RewardRepository
import com.tabitos.app.core.accounting.domain.AccountingRepository
import com.tabitos.app.core.habits.data.HABIT_MIGRATION_1_2
import com.tabitos.app.core.habits.data.RoomHabitRepository
import com.tabitos.app.core.habits.domain.HabitRepository
import com.tabitos.app.core.tasks.domain.TaskClock
import com.tabitos.app.core.tasks.domain.TaskRepository
import com.tabitos.app.core.timers.data.RoomTimerRepository
import com.tabitos.app.core.timers.data.TIMER_MIGRATION_5_6
import com.tabitos.app.core.timers.domain.TimerClock
import com.tabitos.app.core.timers.domain.TimerRepository
import com.tabitos.app.core.timers.platform.AndroidTimerClock
import com.tabitos.app.core.trackers.data.RoomTrackerRepository
import com.tabitos.app.core.trackers.data.TRACKER_MIGRATION_6_7
import com.tabitos.app.core.trackers.domain.TrackerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.tabitos.app.core.preferences.GameModeSource
import com.tabitos.app.core.preferences.TaskReminderDefaultsRepository
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
        Room.databaseBuilder(context, TaskDatabase::class.java, "tabitos-tasks.db")
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
    @Provides @Singleton
    fun habits(database: TaskDatabase, clock: TaskClock, gameMode: GameModeSource): HabitRepository =
        RoomHabitRepository(database, clock, gameMode)
    @Provides @Singleton fun trackers(database: TaskDatabase, clock: TaskClock): TrackerRepository =
        RoomTrackerRepository(database, clock)
    @Provides @Singleton
    fun repository(
        database: TaskDatabase, clock: TaskClock, gameMode: GameModeSource, defaults: TaskReminderDefaultsRepository,
    ): TaskRepository = RoomTaskRepository(database, clock, gameMode, defaults.defaults)
}
