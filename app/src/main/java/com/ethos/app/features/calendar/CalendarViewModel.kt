package com.ethos.app.features.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.habits.domain.HabitRepository
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.tasks.domain.TaskRepository
import com.ethos.app.core.trackers.domain.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CalendarUiState(
    val sources: CalendarSources = CalendarSources(), val today: LocalDate = LocalDate.now(),
    val view: CalendarView = CalendarView.MONTH, val anchor: LocalDate = today,
    val filters: CalendarFilters = CalendarFilters(), val loading: Boolean = true, val error: Boolean = false,
    val cells: List<CalendarCell> = emptyList(), val selected: CalendarDay? = null,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val tasks: TaskRepository, private val habits: HabitRepository, private val trackers: TrackerRepository,
    private val clock: TaskClock, private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(CalendarUiState(today = clock.today(),
        view = saved.get<String>("calendarView")?.let(CalendarView::valueOf) ?: CalendarView.MONTH,
        anchor = saved.get<Long>("calendarAnchor")?.let(LocalDate::ofEpochDay) ?: clock.today(),
        filters = saved["calendarFilters"] ?: CalendarFilters()))
    val state: StateFlow<CalendarUiState> = mutable.asStateFlow()
    private var observer: Job? = null

    init { observe() }

    private fun derive(s: CalendarUiState) = s.copy(
        cells = CalendarModel.cells(s.sources, s.filters, s.view, s.anchor, s.today),
        selected = CalendarModel.day(s.sources, s.filters, s.anchor, s.today))

    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            combine(tasks.observe(), habits.observe(), trackers.observe()) { t, h, r -> CalendarSources(t, h, r) }
                .catch { mutable.update { it.copy(loading = false, error = true) } }
                .collect { sources -> mutable.update { derive(it.copy(sources = sources, loading = false, error = false)) } }
        }
    }

    private fun update(block: (CalendarUiState) -> CalendarUiState) {
        mutable.update { derive(block(it)) }
        val s = mutable.value
        saved["calendarView"] = s.view.name
        saved["calendarAnchor"] = s.anchor.toEpochDay()
        saved["calendarFilters"] = s.filters
    }
    fun view(value: CalendarView) = update { it.copy(view = value) }
    fun select(date: LocalDate) = update { it.copy(anchor = date) }
    fun shift(delta: Long) = update { it.copy(anchor = CalendarModel.shift(it.view, it.anchor, delta)) }
    fun goToday() = update { it.copy(anchor = clock.today(), today = clock.today()) }
    fun filters(value: CalendarFilters) = update { it.copy(filters = value) }
    fun refresh() = mutable.update { derive(it.copy(today = clock.today())) }
    fun retry() { mutable.update { it.copy(error = false, loading = true) }; observe() }
}
