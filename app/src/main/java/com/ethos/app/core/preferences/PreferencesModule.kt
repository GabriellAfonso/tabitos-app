package com.ethos.app.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance")

@Module
@InstallIn(SingletonComponent::class)
abstract class AppearanceBindings {
    @Binds
    @Singleton
    abstract fun bindAppearanceRepository(implementation: DataStoreAppearanceRepository): AppearanceRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.appearanceDataStore
}
