package com.tabitos.app.features.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabitos.app.core.backup.BackupInspection
import com.tabitos.app.core.backup.BackupPreview
import com.tabitos.app.core.backup.BackupRepository
import com.tabitos.app.core.backup.ReplacementRequired
import com.tabitos.app.core.tasks.domain.TaskClock
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BackupMessage { EXPORTED, IMPORTED, EXPORT_FAILED, INSPECTION_FAILED, RESTORE_FAILED, PICKER_FAILED }

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object ExportOptions : BackupUiState
    data object PickingDestination : BackupUiState
    data object Exporting : BackupUiState
    data object Inspecting : BackupUiState
    data object Restoring : BackupUiState
    data class Password(val retry: Boolean) : BackupUiState
    data class Preview(
        val date: String,
        val lists: Int,
        val tasks: Int,
        val habits: Int,
        val replacement: Boolean,
    ) : BackupUiState
    data class Notice(val message: BackupMessage) : BackupUiState
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
    private val clock: TaskClock,
) : ViewModel() {
    private val mutable = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val state = mutable.asStateFlow()
    private var sourceDocument: String? = null
    private var exportPassword: CharArray? = null
    private var inspected: BackupPreview? = null
    private var pendingPreview: BackupUiState.Preview? = null

    fun offerExport() {
        if (mutable.value == BackupUiState.Idle || mutable.value is BackupUiState.Preview) {
            mutable.value = BackupUiState.ExportOptions
        }
    }

    fun prepareExport(password: CharArray?): String? {
        if (mutable.value != BackupUiState.ExportOptions) return null
        exportPassword = password
        mutable.value = BackupUiState.PickingDestination
        val time = localTime(clock.nowMillis()).format(FILE_TIME)
        val extension = if (password == null) ".zip" else ".zip.enc"
        return "Tabitos-backup-$time$extension"
    }

    fun exportTo(document: String?) {
        if (mutable.value != BackupUiState.PickingDestination) return
        val password = exportPassword
        exportPassword = null
        if (document == null) {
            password?.fill('\u0000')
            returnToPreview()
            return
        }
        mutable.value = BackupUiState.Exporting
        viewModelScope.launch {
            try {
                repository.export(document, password)
                mutable.value = BackupUiState.Notice(BackupMessage.EXPORTED)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.value = BackupUiState.Notice(BackupMessage.EXPORT_FAILED)
            } finally {
                password?.fill('\u0000')
            }
        }
    }

    fun openDocument(document: String?) {
        if (mutable.value != BackupUiState.Idle || document == null) return
        sourceDocument = document
        inspect(document, null)
    }

    fun unlock(password: CharArray) {
        if (mutable.value !is BackupUiState.Password) return
        sourceDocument?.let { inspect(it, password) }
    }

    private fun inspect(document: String, password: CharArray?) {
        mutable.value = BackupUiState.Inspecting
        viewModelScope.launch {
            try {
                when (val result = repository.inspect(document, password)) {
                    BackupInspection.PasswordRequired -> mutable.value = BackupUiState.Password(password != null)
                    is BackupInspection.Ready -> showPreview(result.preview)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.value = if (password != null) BackupUiState.Password(retry = true)
                else BackupUiState.Notice(BackupMessage.INSPECTION_FAILED)
            } finally {
                password?.fill('\u0000')
            }
        }
    }

    private fun showPreview(preview: BackupPreview) {
        sourceDocument = null
        inspected = preview
        pendingPreview = BackupUiState.Preview(
            date = localTime(preview.capturedAt).format(PREVIEW_TIME),
            lists = preview.lists,
            tasks = preview.tasks,
            habits = preview.habits,
            replacement = preview.hasPersonalData,
        )
        returnToPreview()
    }

    fun restore() {
        val screen = mutable.value as? BackupUiState.Preview ?: return
        val preview = inspected ?: return
        mutable.value = BackupUiState.Restoring
        viewModelScope.launch {
            try {
                repository.restore(preview, confirmedReplacement = screen.replacement)
                clearPreview()
                mutable.value = BackupUiState.Notice(BackupMessage.IMPORTED)
            } catch (e: CancellationException) {
                throw e
            } catch (_: ReplacementRequired) {
                pendingPreview = screen.copy(replacement = true)
                returnToPreview()
            } catch (_: Exception) {
                mutable.value = BackupUiState.Notice(BackupMessage.RESTORE_FAILED)
            }
        }
    }

    fun pickerFailed() {
        clearExportPassword()
        mutable.value = BackupUiState.Notice(BackupMessage.PICKER_FAILED)
    }

    fun dismiss() {
        when (mutable.value) {
            is BackupUiState.Preview -> clearPreview()
            is BackupUiState.Password, is BackupUiState.Notice -> sourceDocument = null
            BackupUiState.ExportOptions -> Unit
            else -> return
        }
        returnToPreview()
    }

    private fun returnToPreview() { mutable.value = pendingPreview ?: BackupUiState.Idle }

    private fun clearPreview() {
        inspected = null
        pendingPreview = null
    }

    private fun clearExportPassword() {
        exportPassword?.fill('\u0000')
        exportPassword = null
    }

    override fun onCleared() = clearExportPassword()

    private companion object {
        val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.ROOT)
        val PREVIEW_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT)
        fun localTime(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    }
}

/** "Proteger com senha" choice of the export dialog: a protected copy needs a non-empty, repeated password. */
data class ExportProtection(val protect: Boolean, val password: String, val confirmation: String) {
    val showsMismatch: Boolean get() = protect && confirmation.isNotEmpty() && password != confirmation
    val ready: Boolean get() = !protect || (password.isNotEmpty() && password == confirmation)

    /** Password handed to the export, or null for a plain copy. */
    fun password(): CharArray? = if (protect) password.toCharArray() else null
}
