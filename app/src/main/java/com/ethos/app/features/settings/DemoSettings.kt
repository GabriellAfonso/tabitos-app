package com.ethos.app.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethos.app.R
import com.ethos.app.core.ui.theme.EthosTheme

@Composable
fun DemoSettingsScreen(viewModel: DemoViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DemoSettingsContent(state, viewModel::ask, viewModel::cancel, viewModel::restore)
}

@Composable
fun DemoSettingsContent(state: DemoUiState, ask: () -> Unit, cancel: () -> Unit, restore: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.demo_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.demo_hint))
            OutlinedButton(ask, enabled = !state.busy) { Text(stringResource(R.string.demo_reset)) }
            state.result?.let { result ->
                Text(stringResource(when (result) {
                    DemoResult.RESTORED -> R.string.demo_success
                    DemoResult.ERROR -> R.string.demo_error
                    DemoResult.TIMER_RUNNING -> R.string.demo_timer_running
                }), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
    }
    if (state.confirming) AlertDialog(
        onDismissRequest = { if (!state.busy) cancel() },
        title = { Text(stringResource(R.string.demo_reset)) },
        text = { Text(stringResource(if (state.busy) R.string.demo_busy else R.string.demo_confirmation)) },
        confirmButton = {
            TextButton(restore, enabled = !state.busy) { Text(stringResource(R.string.demo_confirm)) }
        },
        dismissButton = { TextButton(cancel, enabled = !state.busy) { Text(stringResource(R.string.cancel)) } },
    )
}

@Preview
@Composable
private fun DemoSettingsPreview() {
    EthosTheme { DemoSettingsContent(DemoUiState(result = DemoResult.RESTORED), {}, {}, {}) }
}
