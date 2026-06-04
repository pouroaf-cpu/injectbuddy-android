package com.injectbuddy.android.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.injectbuddy.android.core.UiState
import com.injectbuddy.android.domain.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Calendar screen data: derived doses keyed by day, plus the selected-day agenda. */
data class CalendarData(
    val visibleMonth: YearMonth,
    val selectedDay: LocalDate,
    /** Every day in (and around) the window that has at least one projected dose. */
    val dosesByDay: Map<LocalDate, List<ProjectedDose>>,
    val today: LocalDate,
) {
    val selectedDoses: List<ProjectedDose> get() = dosesByDay[selectedDay].orEmpty()
}

/**
 * Loads the user's saved dosages once, then projects doses over a window wide enough to
 * cover the months the user can page to. The pure projection lives in [projectDosesByDay];
 * this VM only owns the loading lifecycle + month/day selection. No protocols → Empty.
 */
class CalendarViewModel(
    private val account: AccountRepository,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<CalendarData>>(UiState.Loading)
    val state: StateFlow<UiState<CalendarData>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            account.listSavedDosages()
                .onSuccess { rows ->
                    val protocols = deriveProtocols(rows)
                    if (protocols.isEmpty()) {
                        _state.value = UiState.Empty
                        return@onSuccess
                    }
                    // Project a wide window (from 31 days back to ~120 days forward) so the
                    // grid is populated as the user pages a few months either way.
                    val windowStart = today.minusDays(31)
                    val byDay = projectDoses(protocols, windowStart, windowDays = 152)
                        .groupBy { it.date }
                    _state.value = UiState.Content(
                        CalendarData(
                            visibleMonth = YearMonth.from(today),
                            selectedDay = today,
                            dosesByDay = byDay,
                            today = today,
                        ),
                    )
                }
                .onFailure { t ->
                    _state.value = UiState.Error(t.message ?: "Couldn't load your calendar")
                }
        }
    }

    fun selectDay(day: LocalDate) = updateContent { it.copy(selectedDay = day) }

    fun previousMonth() = updateContent { it.copy(visibleMonth = it.visibleMonth.minusMonths(1)) }

    fun nextMonth() = updateContent { it.copy(visibleMonth = it.visibleMonth.plusMonths(1)) }

    fun goToToday() = updateContent {
        it.copy(visibleMonth = YearMonth.from(today), selectedDay = today)
    }

    private inline fun updateContent(transform: (CalendarData) -> CalendarData) {
        _state.update { current ->
            if (current is UiState.Content) UiState.Content(transform(current.data)) else current
        }
    }
}
