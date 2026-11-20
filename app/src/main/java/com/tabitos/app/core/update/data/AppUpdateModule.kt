package com.tabitos.app.core.update.data

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.tabitos.app.core.update.platform.PlayAppUpdateSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppUpdateBindings {
    @Binds
    @Singleton
    abstract fun bindAppUpdateSource(implementation: PlayAppUpdateSource): AppUpdateSource
}

@Module
@InstallIn(SingletonComponent::class)
object AppUpdateModule {
    @Provides
    @Singleton
    fun provideAppUpdateManager(@ApplicationContext context: Context): AppUpdateManager =
        AppUpdateManagerFactory.create(context)
}
