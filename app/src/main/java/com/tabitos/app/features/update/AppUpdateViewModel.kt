package com.tabitos.app.features.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabitos.app.core.startup.StartupSequence
import com.tabitos.app.core.timers.domain.TimerRepository
import com.tabitos.app.core.update.data.AppUpdateController
import com.tabitos.app.core.update.domain.AppUpdatePhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUpdateUiState(
    /** The store's consent dialog should be started now. */
    val prompt: Boolean = false,
    val notice: UpdateNotice? = null,
)

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val controller: AppUpdateController,
    timers: TimerRepository,
    startup: StartupSequence,
) : ViewModel() {
    /**
     * The strip waits for the first timer reading, so "Reiniciar" never flashes while a timer counts. Without
     * readable timers there is no count to protect, as in the rewards purchase strip.
     */
    private val timerRunning = timers.observe().map { it.running != null }.distinctUntilChanged()
        .catch { emit(false) }

    val state: StateFlow<AppUpdateUiState> = combine(controller.phase, timerRunning) { phase, running ->
        AppUpdateUiState(prompt = phase == AppUpdatePhase.PROMPT, notice = updateNotice(phase, running))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUpdateUiState())

    init {
        // The store is asked only after Hoje is drawn, so the check never delays it (spec 027).
        viewModelScope.launch {
            startup.awaitTodayShown()
            controller.run()
        }
    }

    /** The launcher belongs to the composition; it is only used for this call and never kept. */
    fun launchPrompt(launcher: ActivityResultLauncher<IntentSenderRequest>) = controller.launchPrompt(launcher)

    fun promptResult(accepted: Boolean) = controller.promptResult(accepted)

    fun dismiss() = controller.dismiss()

    /** Installing restarts the app; never while a timer counts, since it would come back paused (spec 008). */
    fun install() {
        if (state.value.notice == UpdateNotice.READY) controller.install()
    }
}
