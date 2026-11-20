package com.tabitos.app.features.backup

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import com.tabitos.app.R
import com.tabitos.app.core.ui.components.SettingsLinkRow
import com.tabitos.app.core.ui.theme.TabitosTheme

private const val BACKUP_MIME = "application/octet-stream"
private val ANY_DOCUMENT = arrayOf("*/*")

/** Callbacks of the backup rows and dialogs, so the content stays free of the ViewModel. */
data class BackupActions(
    val offerExport: () -> Unit,
    val prepareExport: (CharArray?) -> String?,
    val exportTo: (String?) -> Unit,
    val openDocument: (String?) -> Unit,
    val unlock: (CharArray) -> Unit,
    val restore: () -> Unit,
    val pickerFailed: () -> Unit,
    val dismiss: () -> Unit,
)

fun BackupViewModel.actions() = BackupActions(
    offerExport = ::offerExport,
    prepareExport = ::prepareExport,
    exportTo = ::exportTo,
    openDocument = ::openDocument,
    unlock = ::unlock,
    restore = ::restore,
    pickerFailed = ::pickerFailed,
    dismiss = ::dismiss,
)

/** "Exportar backup" and "Importar backup" rows of Configurações → Dados, with their dialogs (spec 037). */
@Composable
fun BackupSettings(state: BackupUiState, actions: BackupActions) {
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) {
        actions.exportTo(it?.toString())
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        actions.openDocument(it?.toString())
    }
    val idle = state == BackupUiState.Idle
    SettingsLinkRow(stringResource(R.string.backup_export), R.drawable.ic_upload, onClick = actions.offerExport,
        supporting = stringResource(R.string.backup_export_supporting), enabled = idle, trailingIcon = null)
    SettingsLinkRow(stringResource(R.string.backup_import), R.drawable.ic_download,
        onClick = { launchPicker(actions.pickerFailed) { openDocument.launch(ANY_DOCUMENT) } },
        supporting = stringResource(R.string.backup_import_supporting), enabled = idle, trailingIcon = null)
    BackupDialog(state, actions) { password ->
        actions.prepareExport(password)?.let { name ->
            launchPicker(actions.pickerFailed) { createDocument.launch(name) }
        }
    }
}

private fun launchPicker(failed: () -> Unit, launch: () -> Unit) {
    try {
        launch()
    } catch (_: ActivityNotFoundException) {
        failed()
    }
}

@Composable
private fun BackupDialog(state: BackupUiState, actions: BackupActions, chooseDestination: (CharArray?) -> Unit) {
    when (state) {
        BackupUiState.Idle, BackupUiState.PickingDestination -> Unit
        BackupUiState.ExportOptions -> ExportOptionsDialog(chooseDestination, actions.dismiss)
        BackupUiState.Exporting -> BusyDialog(R.string.backup_exporting)
        BackupUiState.Inspecting -> BusyDialog(R.string.backup_inspecting)
        BackupUiState.Restoring -> BusyDialog(R.string.backup_restoring)
        is BackupUiState.Password -> PasswordDialog(state.retry, actions.unlock, actions.dismiss)
        is BackupUiState.Preview -> PreviewDialog(state, actions)
        is BackupUiState.Notice -> NoticeDialog(state.message, actions.dismiss)
    }
}

/** Passwords live only in this dialog's memory and are not saved with the screen state. */
@Composable
private fun ExportOptionsDialog(chooseDestination: (CharArray?) -> Unit, dismiss: () -> Unit) {
    var protect by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val choice = ExportProtection(protect, password, confirmation)
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.backup_export)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.backup_export_explanation))
                Row(
                    Modifier.fillMaxWidth().toggleable(protect, role = Role.Checkbox) { protect = it },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(protect, onCheckedChange = null)
                    Text(stringResource(R.string.backup_protect))
                }
                if (protect) {
                    PasswordField(password, R.string.backup_password) { password = it }
                    PasswordField(confirmation, R.string.backup_password_confirmation,
                        error = choice.showsMismatch) { confirmation = it }
                    if (choice.showsMismatch) {
                        Text(stringResource(R.string.backup_password_mismatch),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                }
                Text(stringResource(if (protect) R.string.backup_protected_warning
                    else R.string.backup_unprotected_warning), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { chooseDestination(choice.password()) }, enabled = choice.ready) {
                Text(stringResource(R.string.backup_choose_destination))
            }
        },
        dismissButton = { TextButton(dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun PasswordField(value: String, label: Int, error: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = error,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordDialog(retry: Boolean, unlock: (CharArray) -> Unit, dismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.backup_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.backup_password_prompt))
                PasswordField(password, R.string.backup_password, error = retry) { password = it }
                if (retry) {
                    Text(stringResource(R.string.backup_password_wrong), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { unlock(password.toCharArray()) }, enabled = password.isNotEmpty()) {
                Text(stringResource(R.string.backup_open))
            }
        },
        dismissButton = { TextButton(dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun PreviewDialog(preview: BackupUiState.Preview, actions: BackupActions) {
    AlertDialog(
        onDismissRequest = actions.dismiss,
        title = { Text(stringResource(R.string.backup_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.backup_preview_date, preview.date),
                    style = MaterialTheme.typography.titleSmall)
                Text(pluralStringResource(R.plurals.backup_preview_lists, preview.lists, preview.lists))
                Text(pluralStringResource(R.plurals.backup_preview_tasks, preview.tasks, preview.tasks))
                Text(pluralStringResource(R.plurals.backup_preview_habits, preview.habits, preview.habits))
                if (preview.replacement) {
                    Text(stringResource(R.string.backup_replace_warning), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            if (preview.replacement) {
                TextButton(actions.restore,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.backup_confirm_replace))
                }
            } else {
                TextButton(actions.restore) { Text(stringResource(R.string.backup_confirm_import)) }
            }
        },
        dismissButton = {
            Row {
                TextButton(actions.dismiss) { Text(stringResource(R.string.cancel)) }
                if (preview.replacement) {
                    TextButton(actions.offerExport) { Text(stringResource(R.string.backup_export)) }
                }
            }
        },
    )
}

@Composable
private fun BusyDialog(message: Int) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Text(stringResource(message), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        },
    )
}

@Composable
private fun NoticeDialog(message: BackupMessage, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        text = { Text(stringResource(message.text)) },
        confirmButton = { TextButton(dismiss) { Text(stringResource(R.string.close)) } },
    )
}

private val BackupMessage.text: Int
    get() = when (this) {
        BackupMessage.EXPORTED -> R.string.backup_exported
        BackupMessage.IMPORTED -> R.string.backup_imported
        BackupMessage.EXPORT_FAILED -> R.string.backup_export_failed
        BackupMessage.INSPECTION_FAILED -> R.string.backup_inspection_failed
        BackupMessage.RESTORE_FAILED -> R.string.backup_restore_failed
        BackupMessage.PICKER_FAILED -> R.string.backup_picker_failed
    }

private val previewActions = BackupActions({}, { null }, {}, {}, {}, {}, {}, {})

@Preview
@Composable
private fun BackupReplacementPreview() {
    TabitosTheme {
        PreviewDialog(BackupUiState.Preview("19/11/2026 14:30", lists = 4, tasks = 37, habits = 6,
            replacement = true), previewActions)
    }
}

@Preview
@Composable
private fun BackupRowsPreview() {
    TabitosTheme { Column { BackupSettings(BackupUiState.Idle, previewActions) } }
}
