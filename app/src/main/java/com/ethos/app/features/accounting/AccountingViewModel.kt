package com.ethos.app.features.accounting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.accounting.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AccountingUiState(
    val snapshot: AccountingSnapshot = AccountingSnapshot(), val loading: Boolean = true,
    val error: Boolean = false, val saving: Boolean = false, val invalid: Boolean = false,
    val draft: List<String>? = null,
)

@HiltViewModel
class AccountingViewModel @Inject constructor(
    private val repository: AccountingRepository, private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(AccountingUiState(draft = saved.get<ArrayList<String>>("prices")))
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private var failedSave = false
    private var readFailed = false
    init { observe() }

    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            try {
                repository.observe().collect { snapshot ->
                    readFailed = false
                    mutable.update { it.copy(snapshot = snapshot, loading = false, error = failedSave) }
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                readFailed = true
                mutable.update { it.copy(loading = false, error = true) }
            }
        }
    }

    fun edit() {
        draft(state.value.snapshot.prices.flatMap {
            listOf(it.coins.toString(), it.xp.toString(), it.penalty.toString())
        })
    }
    fun draft(values: List<String>?) {
        if (values == null) failedSave = false
        saved["prices"] = values?.let { ArrayList(it) }
        mutable.update { it.copy(draft = values, invalid = false, error = readFailed || failedSave) }
    }
    fun save() {
        if (state.value.saving || readFailed) return
        val values = state.value.draft ?: return
        val prices = try {
            require(values.size == 9 && values.all { it.isNotEmpty() && it.all(Char::isDigit) })
            (0..2).map { i -> Price(i, values[i * 3].toLong(), values[i * 3 + 1].toLong(),
                values[i * 3 + 2].toLong()).also { it.validate() } }
        } catch (_: IllegalArgumentException) {
            mutable.update { it.copy(invalid = true) }; return
        }
        mutable.update { it.copy(saving = true, invalid = false, error = false) }
        viewModelScope.launch {
            try {
                repository.savePrices(prices)
                failedSave = false
                draft(null)
                mutable.update { it.copy(saving = false, error = readFailed) }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                failedSave = true
                mutable.update { it.copy(saving = false, error = true) }
            }
        }
    }
    fun retry() { observe(); if (failedSave) save() }
}
