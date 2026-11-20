package com.tabitos.app.core.update.data

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.tabitos.app.core.update.domain.AppUpdateEvent
import com.tabitos.app.core.update.domain.AppUpdatePhase
import com.tabitos.app.core.update.domain.AppUpdateRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The update flow of one process (spec 039). Being a singleton, a recreated or reopened Activity in the same process
 * neither asks the store again nor shows a prompt the user already declined; only a new process does.
 */
@Singleton
class AppUpdateController @Inject constructor(private val source: AppUpdateSource) {
    private val mutable = MutableStateFlow(AppUpdatePhase.IDLE)
    val phase: StateFlow<AppUpdatePhase> = mutable.asStateFlow()

    private val checkLock = Mutex()
    private var checked = false
    /** Guards against starting the consent dialog twice, e.g. when the Activity is recreated while it is open. */
    private var promptLaunched = false

    /** Checks the store once per process, then follows the download for as long as the caller's scope lives. */
    suspend fun run() {
        checkLock.withLock {
            if (!checked) {
                apply(AppUpdateEvent.Checked(source.check()))
                checked = true
            }
        }
        source.installStates().collect { apply(AppUpdateEvent.InstallState(it)) }
    }

    fun launchPrompt(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (phase.value != AppUpdatePhase.PROMPT || promptLaunched) return
        promptLaunched = true
        if (!source.startFlexible(launcher)) apply(AppUpdateEvent.PromptAnswered(accepted = false))
    }

    fun promptResult(accepted: Boolean) = apply(AppUpdateEvent.PromptAnswered(accepted))

    fun dismiss() = apply(AppUpdateEvent.NotNow)

    fun install() {
        if (phase.value == AppUpdatePhase.READY) source.complete()
    }

    private fun apply(event: AppUpdateEvent) = mutable.update { AppUpdateRules.next(it, event) }
}
