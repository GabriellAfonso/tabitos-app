package com.tabitos.app.features.update

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.ui.theme.TabitosTheme

/** Starts the store's consent dialog when the ViewModel asks for it; its answer goes back to [onResult]. */
@Composable
fun AppUpdatePromptEffect(
    prompt: Boolean,
    launch: (ActivityResultLauncher<IntentSenderRequest>) -> Unit,
    onResult: (accepted: Boolean) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        onResult(it.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(prompt) { if (prompt) launch(launcher) }
}

data class AppUpdateStripActions(val restart: () -> Unit, val notNow: () -> Unit)

/** "Atualização pronta" (spec 039): stays until an action, unlike the timed messages of the shell. */
@Composable
fun AppUpdateStrip(notice: UpdateNotice, actions: AppUpdateStripActions) {
    Snackbar(
        Modifier.padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        actionContentColor = MaterialTheme.colorScheme.primary,
        dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        action = if (notice == UpdateNotice.READY) {
            { TextButton(actions.restart) { Text(stringResource(R.string.update_restart)) } }
        } else null,
        dismissAction = { TextButton(actions.notNow) { Text(stringResource(R.string.update_not_now)) } },
    ) {
        Column {
            Text(stringResource(R.string.update_ready), style = MaterialTheme.typography.bodyMedium)
            if (notice == UpdateNotice.WAITING_TIMER) {
                Text(stringResource(R.string.update_waiting_timer), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview
@Composable
private fun AppUpdateStripReadyPreview() {
    TabitosTheme { AppUpdateStrip(UpdateNotice.READY, AppUpdateStripActions({}, {})) }
}

@Preview
@Composable
private fun AppUpdateStripWaitingDarkPreview() {
    TabitosTheme(darkTheme = true) { AppUpdateStrip(UpdateNotice.WAITING_TIMER, AppUpdateStripActions({}, {})) }
}
