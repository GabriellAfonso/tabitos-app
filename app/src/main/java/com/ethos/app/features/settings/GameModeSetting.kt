package com.ethos.app.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.R
import com.ethos.app.core.preferences.GameModeRepository
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.timers.domain.TimerRepository
import com.ethos.app.core.timers.domain.TimerSnapshot
import com.ethos.app.core.timers.domain.TimerState
import com.ethos.app.core.ui.theme.EthosTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameModeUiState(
    val enabled: Boolean = true,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val confirmingOff: Boolean = false,
    /** A counting reward purchase would be hidden mid-session, so switching waits for it (spec 017). */
    val rewardTimerRunning: Boolean = false,
    val error: Boolean = false,
) {
    val canSwitch: Boolean get() = !loading && !saving && !rewardTimerRunning
}

/** "Modo jogo" switch (spec 017): turning off asks first; turning on applies directly. */
@HiltViewModel
class GameModeViewModel @Inject constructor(
    private val repository: GameModeRepository,
    timers: TimerRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow(GameModeUiState())
    val state: StateFlow<GameModeUiState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            try { repository.enabled.collect { on -> mutable.update { it.copy(enabled = on, loading = false) } }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { mutable.update { it.copy(loading = false, error = true) } }
        }
        viewModelScope.launch {
            try { timers.observe().collect { snapshot ->
                mutable.update { it.copy(rewardTimerRunning = snapshot.hasRunningReward()) } }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { }
        }
    }

    fun request(enabled: Boolean) {
        if (!state.value.canSwitch || enabled == state.value.enabled) return
        if (enabled) save(true) else mutable.update { it.copy(confirmingOff = true) }
    }

    fun confirmOff(confirm: Boolean) {
        mutable.update { it.copy(confirmingOff = false) }
        if (confirm && state.value.canSwitch) save(false)
    }

    private fun save(enabled: Boolean) {
        mutable.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            try {
                repository.setEnabled(enabled)
                mutable.update { it.copy(enabled = enabled, saving = false) }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { mutable.update { it.copy(saving = false, error = true) } }
        }
    }
}

private fun TimerSnapshot.hasRunningReward(): Boolean =
    sessions.any { it.kind == TimerKind.REWARD && it.state == TimerState.RUNNING }

@Composable
fun GameModeSetting(state: GameModeUiState, onRequest: (Boolean) -> Unit, onConfirmOff: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp)
        .toggleable(state.enabled, enabled = state.canSwitch, role = Role.Switch, onValueChange = onRequest),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.game_mode_setting), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.game_mode_setting_hint), style = MaterialTheme.typography.bodySmall)
        }
        Switch(state.enabled, onCheckedChange = null, enabled = state.canSwitch)
    }
    val notice = when {
        state.rewardTimerRunning -> R.string.game_mode_reward_timer_running
        state.error -> R.string.game_mode_error
        else -> null
    }
    if (notice != null) {
        Text(stringResource(notice), Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    if (state.confirmingOff) AlertDialog(
        onDismissRequest = { onConfirmOff(false) },
        title = { Text(stringResource(R.string.game_mode_off_title)) },
        text = { Text(stringResource(R.string.game_mode_off_message)) },
        confirmButton = { TextButton({ onConfirmOff(true) }) { Text(stringResource(R.string.game_mode_off_confirm)) } },
        dismissButton = { TextButton({ onConfirmOff(false) }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Preview
@Composable
private fun GameModeSettingPreview() {
    EthosTheme { Column { GameModeSetting(GameModeUiState(enabled = true, loading = false), {}, {}) } }
}

@Preview
@Composable
private fun GameModeBlockedPreview() {
    EthosTheme(darkTheme = true) {
        Column {
            GameModeSetting(GameModeUiState(enabled = true, loading = false, rewardTimerRunning = true), {}, {})
        }
    }
}
