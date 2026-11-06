package com.ethos.app.core.tasks.data

import android.content.Context
import androidx.room.Room
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
        Room.databaseBuilder(context, TaskDatabase::class.java, "ethos-tasks.db").build()
    @Provides @Singleton fun repository(database: TaskDatabase, clock: TaskClock): TaskRepository =
        RoomTaskRepository(database, clock)
}
