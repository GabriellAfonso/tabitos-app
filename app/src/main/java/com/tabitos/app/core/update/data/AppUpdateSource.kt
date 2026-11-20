package com.tabitos.app.core.update.data

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.tabitos.app.core.update.domain.StoreUpdate
import kotlinx.coroutines.flow.Flow

/** The store's in-app update service, flexible mode only (spec 039). */
interface AppUpdateSource {
    /** One query to the store; never throws. Failures and non-store installs are [StoreUpdate.NOT_AVAILABLE]. */
    suspend fun check(): StoreUpdate

    /** Download progress of a flexible update; the listener is removed when collection stops. */
    fun installStates(): Flow<StoreUpdate>

    /** Starts the store's consent flow for the update found by the last [check]; false if it could not start. */
    fun startFlexible(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean

    /** Installs a downloaded update; the store restarts the app. */
    fun complete()
}
