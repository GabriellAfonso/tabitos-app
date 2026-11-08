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
                REMINDER_MIGRATION_4_5).build()
    @Provides @Singleton fun reminders(repository: RoomReminderRepository): ReminderRepository = repository
    @Provides @Singleton fun rewards(database: TaskDatabase, clock: TaskClock): RewardRepository =
        RoomRewardRepository(database, clock)
    @Provides @Singleton fun accounting(database: TaskDatabase, clock: TaskClock): AccountingRepository =
        RoomAccountingRepository(database, clock)
    @Provides @Singleton fun habits(database: TaskDatabase, clock: TaskClock): HabitRepository =
        RoomHabitRepository(database, clock)
    @Provides @Singleton fun repository(database: TaskDatabase, clock: TaskClock): TaskRepository =
        RoomTaskRepository(database, clock)
}
