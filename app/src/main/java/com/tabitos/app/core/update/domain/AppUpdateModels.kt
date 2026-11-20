package com.tabitos.app.core.update.domain

/** What the store reports about a newer version of the app (spec 039). */
enum class StoreUpdate {
    /** No newer version, a non-store install, or the store could not be reached. */
    NOT_AVAILABLE,
    /** A newer version exists and may be downloaded in the background. */
    AVAILABLE,
    DOWNLOADING,
    /** Downloaded and waiting to be installed, possibly since an earlier opening. */
    DOWNLOADED,
    /** The download failed or was cancelled; only reported while following a download. */
    FAILED,
}

/** Where this opening of the app is in the update flow; kept in memory only. */
enum class AppUpdatePhase {
    IDLE,
    /** The store's consent dialog should be shown, once. */
    PROMPT,
    DOWNLOADING,
    /** "Atualização pronta" is shown. */
    READY,
    /** Declined or "Agora não": nothing more until the next opening. */
    DISMISSED,
}

sealed interface AppUpdateEvent {
    data class Checked(val update: StoreUpdate) : AppUpdateEvent
    data class InstallState(val update: StoreUpdate) : AppUpdateEvent
    data class PromptAnswered(val accepted: Boolean) : AppUpdateEvent
    data object NotNow : AppUpdateEvent
}

object AppUpdateRules {
    fun next(phase: AppUpdatePhase, event: AppUpdateEvent): AppUpdatePhase {
        // Declining is final for this opening, even if a download still finishes in the background.
        if (phase == AppUpdatePhase.DISMISSED) return phase
        return when (event) {
            is AppUpdateEvent.Checked -> afterCheck(phase, event.update)
            is AppUpdateEvent.InstallState -> afterInstallState(phase, event.update)
            is AppUpdateEvent.PromptAnswered -> afterPrompt(phase, event.accepted)
            AppUpdateEvent.NotNow -> if (phase == AppUpdatePhase.READY) AppUpdatePhase.DISMISSED else phase
        }
    }

    private fun afterCheck(phase: AppUpdatePhase, update: StoreUpdate): AppUpdatePhase {
        if (phase != AppUpdatePhase.IDLE) return phase
        return when (update) {
            StoreUpdate.AVAILABLE -> AppUpdatePhase.PROMPT
            StoreUpdate.DOWNLOADING -> AppUpdatePhase.DOWNLOADING
            StoreUpdate.DOWNLOADED -> AppUpdatePhase.READY
            StoreUpdate.NOT_AVAILABLE, StoreUpdate.FAILED -> AppUpdatePhase.IDLE
        }
    }

    private fun afterInstallState(phase: AppUpdatePhase, update: StoreUpdate): AppUpdatePhase {
        // While the consent dialog is open, only its answer moves the flow.
        if (phase == AppUpdatePhase.PROMPT) return phase
        return when (update) {
            StoreUpdate.DOWNLOADING -> AppUpdatePhase.DOWNLOADING
            StoreUpdate.DOWNLOADED -> AppUpdatePhase.READY
            StoreUpdate.FAILED -> if (phase == AppUpdatePhase.DOWNLOADING) AppUpdatePhase.IDLE else phase
            StoreUpdate.NOT_AVAILABLE, StoreUpdate.AVAILABLE -> phase
        }
    }

    private fun afterPrompt(phase: AppUpdatePhase, accepted: Boolean): AppUpdatePhase = when {
        phase != AppUpdatePhase.PROMPT -> phase
        accepted -> AppUpdatePhase.DOWNLOADING
        else -> AppUpdatePhase.DISMISSED
    }
}
