package com.tabitos.app.core.update.platform

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.tabitos.app.core.update.data.AppUpdateSource
import com.tabitos.app.core.update.domain.StoreUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Google Play In-App Updates; any store failure is reported as "no update" so the app never shows an error. */
@Singleton
class PlayAppUpdateSource @Inject constructor(private val manager: AppUpdateManager) : AppUpdateSource {
    private val flexible = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
    /** The store needs the same info object that reported the update to start its flow. */
    private var available: AppUpdateInfo? = null

    override suspend fun check(): StoreUpdate = try {
        val info = manager.requestAppUpdateInfo()
        val update = statusOf(info)
        available = if (update == StoreUpdate.AVAILABLE) info else null
        update
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        StoreUpdate.NOT_AVAILABLE
    }

    override fun installStates(): Flow<StoreUpdate> = callbackFlow {
        val listener = InstallStateUpdatedListener { state -> installUpdate(state.installStatus())?.let(::trySend) }
        try {
            manager.registerListener(listener)
        } catch (_: Exception) {
            close()
        }
        awaitClose { manager.unregisterListener(listener) }
    }

    override fun startFlexible(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        val info = available ?: return false
        available = null
        return try {
            manager.startUpdateFlowForResult(info, launcher, flexible)
        } catch (_: Exception) {
            false
        }
    }

    override fun complete() {
        try {
            manager.completeUpdate()
        } catch (_: Exception) {
            // The store keeps the download; the next opening offers it again.
        }
    }

    private fun statusOf(info: AppUpdateInfo): StoreUpdate {
        // A download in progress or finished earlier wins; an earlier failure just means offering it again.
        installUpdate(info.installStatus())?.takeUnless { it == StoreUpdate.FAILED }?.let { return it }
        val newer = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
        return if (newer && info.isUpdateTypeAllowed(flexible)) StoreUpdate.AVAILABLE else StoreUpdate.NOT_AVAILABLE
    }

    private fun installUpdate(status: Int): StoreUpdate? = when (status) {
        InstallStatus.PENDING, InstallStatus.DOWNLOADING -> StoreUpdate.DOWNLOADING
        InstallStatus.DOWNLOADED -> StoreUpdate.DOWNLOADED
        InstallStatus.FAILED, InstallStatus.CANCELED -> StoreUpdate.FAILED
        else -> null
    }
}
