package com.injectbuddy.android.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.injectbuddy.android.core.UiState
import com.injectbuddy.android.data.model.Cycle
import com.injectbuddy.android.data.model.SavedDosage
import com.injectbuddy.android.data.model.UserProfile
import com.injectbuddy.android.domain.AccountRepository
import com.injectbuddy.android.domain.CycleRepository
import com.injectbuddy.android.feature.calendar.DerivedProtocol
import com.injectbuddy.android.feature.calendar.ProjectedDose
import com.injectbuddy.android.feature.calendar.deriveProtocols
import com.injectbuddy.android.feature.calendar.isDoseDay
import com.injectbuddy.android.feature.calendar.projectDoses
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Everything the dashboard renders, derived once in the VM so the UI stays dumb. */
data class DashboardData(
    val profile: UserProfile?,
    val greeting: String,
    val dosages: List<SavedDosage>,
    val protocols: List<DerivedProtocol>,
    val activeCycle: Cycle?,
    val nextDose: ProjectedDose?,
    val nextDoseCountdownDays: Long?,
    val cycleDay: Int?,
    val cycleTotalDays: Int?,
    /** Mon→Sun of the current week, each flagged with the protocols dosing that day. */
    val weekStrip: List<DayDoses>,
    /** Human "100mg test · 0.5mg sema" style weekly totals per protocol. */
    val weeklyTotals: List<String>,
)

data class DayDoses(val date: LocalDate, val isToday: Boolean, val doseCount: Int)

/**
 * Loads profile + saved dosages + cycles in PARALLEL (independent queries → async), then
 * derives the dashboard's higher-level views (next dose, week strip, weekly totals) off
 * the projection engine shared with the calendar. Errors collapse to UiState.Error with a
 * Retry; no protocols → UiState.Empty so the screen can show the "add your first" CTA.
 */
class DashboardViewModel(
    private val account: AccountRepository,
    private val cycles: CycleRepository,
    private val today: LocalDate = LocalDate.now(),
    private val hourOfDay: Int = java.time.LocalTime.now().hour,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val state: StateFlow<UiState<DashboardData>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val profileDeferred = async { account.getProfile() }
                    val dosagesDeferred = async { account.listSavedDosages() }
                    val cyclesDeferred = async { cycles.listCycles() }

                    val profile = profileDeferred.await().getOrNull()
                    // A failed dosage load is the load-bearing failure; surface it.
                    val dosages = dosagesDeferred.await().getOrThrow()
                    val cycleList = cyclesDeferred.await().getOrElse { emptyList() }
                    Triple(profile, dosages, cycleList)
                }
            }.onSuccess { (profile, dosages, cycleList) ->
                _state.value = build(profile, dosages, cycleList)
            }.onFailure { t ->
                _state.value = UiState.Error(t.message ?: "Couldn't load your dashboard")
            }
        }
    }

    private fun build(
        profile: UserProfile?,
        dosages: List<SavedDosage>,
        cycleList: List<Cycle>,
    ): UiState<DashboardData> {
        val protocols = deriveProtocols(dosages)
        if (protocols.isEmpty()) return UiState.Empty

        // Next dose: the earliest projected dose from today forward (14-day look-ahead).
        val upcoming = projectDoses(protocols, today, windowDays = 14)
        val nextDose = upcoming.firstOrNull()
        val countdown = nextDose?.let { ChronoUnit.DAYS.between(today, it.date) }

        val activeCycle = cycleList.firstOrNull { it.isActive } ?: cycleList.firstOrNull()
        val (cycleDay, cycleTotal) = cycleProgress(activeCycle)

        return UiState.Content(
            DashboardData(
                profile = profile,
                greeting = greeting(),
                dosages = dosages,
                protocols = protocols,
                activeCycle = activeCycle,
                nextDose = nextDose,
                nextDoseCountdownDays = countdown,
                cycleDay = cycleDay,
                cycleTotalDays = cycleTotal,
                weekStrip = weekStrip(protocols),
                weeklyTotals = weeklyTotals(protocols),
            ),
        )
    }

    /** Mon-anchored 7-day strip with a dose count per day (drives CycleTimelineRow). */
    private fun weekStrip(protocols: List<DerivedProtocol>): List<DayDoses> {
        val monday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
        return (0 until 7).map { i ->
            val date = monday.plusDays(i.toLong())
            DayDoses(
                date = date,
                isToday = date == today,
                doseCount = protocols.count { isDoseDay(it, date) },
            )
        }
    }

    /**
     * One human label per protocol: doses-this-week × per-dose label. We can't always sum
     * mg (labels are pre-formatted strings), so we show "N× {doseLabel} {name}" — honest
     * about what the data supports without fabricating a combined total.
     */
    private fun weeklyTotals(protocols: List<DerivedProtocol>): List<String> {
        val monday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
        return protocols.map { p ->
            val count = (0 until 7).count { isDoseDay(p, monday.plusDays(it.toLong())) }
            "$count× ${p.doseLabel} · ${p.label}"
        }
    }

    /** Cycle progress in days, when start_date + on_weeks are present. */
    private fun cycleProgress(cycle: Cycle?): Pair<Int?, Int?> {
        if (cycle == null) return null to null
        val start = com.injectbuddy.android.feature.calendar.parseLocalDate(cycle.startDate)
            ?: return null to null
        val total = cycle.onWeeks?.let { it * 7 }
        val day = (ChronoUnit.DAYS.between(start, today) + 1).toInt().coerceAtLeast(1)
        return day to total
    }

    private fun greeting(): String = when {
        hourOfDay < 12 -> "Good morning"
        hourOfDay < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}
