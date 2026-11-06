package com.ethos.app.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.preferences.AppearanceMode
import com.ethos.app.core.preferences.AppearanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppearanceError { READ, WRITE }

data class AppearanceUiState(
    val mode: AppearanceMode = AppearanceMode.SYSTEM,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: AppearanceError? = null,
    val attemptedMode: AppearanceMode? = null,
)

@HiltViewModel
class AppearanceViewModel @Inject constructor(private val repository: AppearanceRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AppearanceUiState())
    val state = mutableState.asStateFlow()
    private var reading: Job? = null

    init { read() }

    private fun read() {
        reading?.cancel()
        mutableState.update { it.copy(loading = true, error = null) }
        reading = viewModelScope.launch {
            try {
                repository.modes.collect { mode ->
                    mutableState.update { it.copy(mode = mode, loading = false) }
                }
            } catch (_: IOException) {
                mutableState.update {
                    it.copy(mode = AppearanceMode.SYSTEM, loading = false, error = AppearanceError.READ)
                }
            }
        }
    }

    fun select(mode: AppearanceMode) {
        val current = state.value
        if (current.loading || current.saving || current.error == AppearanceError.READ) return
        mutableState.update { it.copy(saving = true, error = null, attemptedMode = mode) }
        viewModelScope.launch {
            try {
                repository.setMode(mode)
                mutableState.update { it.copy(mode = mode, saving = false, attemptedMode = null) }
            } catch (_: IOException) {
                mutableState.update { it.copy(saving = false, error = AppearanceError.WRITE) }
            }
        }
    }

    fun retry() {
        when (state.value.error) {
            AppearanceError.READ -> read()
            AppearanceError.WRITE -> state.value.attemptedMode?.let(::select)
            null -> Unit
        }
    }
}
