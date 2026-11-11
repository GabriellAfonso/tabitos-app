package com.ethos.app.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.demo.domain.DemoRepository
import com.ethos.app.core.demo.domain.DemoTimerRunningException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DemoResult { RESTORED, ERROR, TIMER_RUNNING }

data class DemoUiState(val confirming: Boolean = false, val busy: Boolean = false, val result: DemoResult? = null)

@HiltViewModel
class DemoViewModel @Inject constructor(private val repository: DemoRepository) : ViewModel() {
    private val mutable = MutableStateFlow(DemoUiState())
    val state = mutable.asStateFlow()

    fun ask() { if (!state.value.busy) mutable.value = DemoUiState(confirming = true) }
    fun cancel() { if (!state.value.busy) mutable.value = DemoUiState() }
    fun restore() {
        if (!state.value.confirming || state.value.busy) return
        mutable.value = DemoUiState(confirming = true, busy = true)
        viewModelScope.launch {
            val result = try {
                repository.restore()
                DemoResult.RESTORED
            } catch (e: CancellationException) { throw e
            } catch (_: DemoTimerRunningException) { DemoResult.TIMER_RUNNING
            } catch (_: Exception) { DemoResult.ERROR }
            mutable.value = DemoUiState(result = result)
        }
    }
}
