@file:OptIn(ExperimentalMaterial3Api::class)

package com.injectbuddy.android.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.injectbuddy.android.core.UiState
import com.injectbuddy.android.data.model.UserProfile
import com.injectbuddy.android.di.ServiceLocator
import com.injectbuddy.android.feature.calendar.DerivedProtocol
import com.injectbuddy.android.feature.calendar.ProjectedDose
import com.injectbuddy.android.ui.components.EmptyState
import com.injectbuddy.android.ui.components.ErrorState
import com.injectbuddy.android.ui.components.LoadingState
import com.injectbuddy.android.ui.theme.ThemeMode
import java.time.LocalDate

/**
 * Dashboard / cycle-planner — the NavHost start destination (SCREENS.md §1). Owns its own
 * Scaffold + TopAppBar (menu → drawer, action → theme cycle). Body is a single
 * LazyColumn-free scroll column of cards: greeting, NextDoseCard, CycleTimelineRow,
 * Protocols grid, StatsIsland. Loading/Empty/Error map to the shared state views.
 *
 * Signature is fixed — AppNavHost calls DashboardScreen(openDrawer, onOpenCalculator).
 */
@Composable
fun DashboardScreen(
    openDrawer: () -> Unit,
    onOpenCalculator: (String) -> Unit,
) {
    val vm: DashboardViewModel = viewModel {
        DashboardViewModel(ServiceLocator.accountRepository, ServiceLocator.cycleRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val themeMode by ServiceLocator.themeController.mode.collectAsStateWithLifecycle()

    DashboardContent(
        state = state,
        themeMode = themeMode,
        openDrawer = openDrawer,
        onOpenCalculator = onOpenCalculator,
        onRetry = vm::load,
        onToggleTheme = { ServiceLocator.themeController.set(themeMode.next()) },
    )
}

/**
 * Stateless dashboard UI — the full Scaffold + TopAppBar + body, fed [state] and lambdas.
 * Pure (no VM / ServiceLocator) so it renders headlessly under Paparazzi.
 */
@Composable
internal fun DashboardContent(
    state: UiState<DashboardData>,
    themeMode: ThemeMode,
    openDrawer: () -> Unit,
    onOpenCalculator: (String) -> Unit,
    onRetry: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("injectbuddy") },
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        val icon = if (themeMode == ThemeMode.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode
                        Icon(icon, contentDescription = "Toggle theme")
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            is UiState.Loading -> LoadingState(Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, onRetry = onRetry, modifier = Modifier.padding(padding))
            is UiState.Empty -> EmptyState(
                title = "No protocols yet",
                message = "Add your first protocol to see your dose schedule and weekly totals.",
                actionLabel = "Add your first protocol",
                onAction = { onOpenCalculator("trt-dose") },
                modifier = Modifier.padding(padding),
            )
            is UiState.Content -> DashboardBody(
                data = s.data,
                contentPadding = padding,
                onOpenCalculator = onOpenCalculator,
            )
        }
    }
}

@Composable
private fun DashboardBody(
    data: DashboardData,
    contentPadding: PaddingValues,
    onOpenCalculator: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val name = data.profile?.displayName ?: data.profile?.email?.substringBefore('@') ?: "there"
        Text(
            "${data.greeting}, $name",
            style = MaterialTheme.typography.headlineSmall,
        )

        data.nextDose?.let { dose ->
            NextDoseCard(
                title = dose.label,
                doseLabel = dose.doseLabel,
                countdownDays = data.nextDoseCountdownDays,
            )
        }

        if (data.weekStrip.isNotEmpty()) {
            SectionHeader("THIS CYCLE")
            CycleTimelineRow(
                week = data.weekStrip,
                cycleDay = data.cycleDay,
                cycleTotalDays = data.cycleTotalDays,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader("PROTOCOLS")
            IconButton(onClick = { onOpenCalculator("trt-dose") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add protocol")
            }
        }
        ProtocolGrid(
            protocols = data.protocols,
            onClick = { protocol -> onOpenCalculator(calculatorSlug(protocol.calculatorType)) },
        )

        if (data.weeklyTotals.isNotEmpty()) {
            SectionHeader("WEEK AT A GLANCE")
            StatsIsland(totals = data.weeklyTotals)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NextDoseCard(title: String, doseLabel: String, countdownDays: Long?) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("NEXT DOSE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge)
            val countdown = when (countdownDays) {
                null -> ""
                0L -> " · due today"
                1L -> " · due tomorrow"
                else -> " · in ${countdownDays}d"
            }
            Text("$doseLabel$countdown", style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = { /* TODO(dose-log): optimistic "mark taken" — no dose_history table yet */ }) {
                Text("Mark taken")
            }
        }
    }
}

@Composable
private fun CycleTimelineRow(week: List<DayDoses>, cycleDay: Int?, cycleTotalDays: Int?) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(week) { day -> DayCell(day) }
            }
            if (cycleDay != null && cycleTotalDays != null && cycleTotalDays > 0) {
                Text(
                    "Day $cycleDay of $cycleTotalDays",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { (cycleDay.toFloat() / cycleTotalDays).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DayCell(day: DayDoses) {
    val label = day.date.dayOfWeek.name.take(1) // M/T/W…
    Surface(
        shape = CircleShape,
        color = if (day.isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(40.dp),
    ) {
        Column(
            Modifier.padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                if (day.doseCount > 0) "●" else "·",
                style = MaterialTheme.typography.labelSmall,
                color = if (day.doseCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProtocolGrid(protocols: List<DerivedProtocol>, onClick: (DerivedProtocol) -> Unit) {
    // Nested fixed-height grid inside the scroll column — height bounded so it doesn't
    // fight the parent vertical scroll (two-wide, ~2.5 rows visible before its own scroll).
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(protocols, key = { it.id }) { protocol ->
            ProtocolCard(protocol = protocol, onClick = { onClick(protocol) })
        }
    }
}

@Composable
private fun ProtocolCard(protocol: DerivedProtocol, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                protocol.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${protocol.doseLabel} · q${formatFreq(protocol.freqDays)}d",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatsIsland(totals: List<String>) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            totals.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun formatFreq(freqDays: Double): String =
    if (freqDays == Math.floor(freqDays)) freqDays.toInt().toString() else freqDays.toString()

private fun ThemeMode.next(): ThemeMode = when (this) {
    ThemeMode.SYSTEM -> ThemeMode.LIGHT
    ThemeMode.LIGHT -> ThemeMode.DARK
    ThemeMode.DARK -> ThemeMode.SYSTEM
}

/**
 * Map a saved row's calculator_type (the web's config id, e.g. "trt", "eod", "peptide")
 * to the native calculator route slug from NavItems. The web stores short ids; the app's
 * routes use hyphenated slugs. Unknown types fall through unchanged.
 */
private fun calculatorSlug(calculatorType: String): String = when (calculatorType) {
    "trt" -> "trt-dose"
    "eod" -> "trt-eod"
    "bpc157" -> "bpc-157"
    "bpc157blend", "blend" -> "bpc-157-tb500"
    "freetest" -> "free-t-index"
    else -> calculatorType
}

// ── sample states (for Paparazzi snapshots) ────────────────────────────────────────

/**
 * A fully-populated dashboard: greeting, a next-dose card, a Mon→Sun week strip with dose
 * dots, three protocol cards, and weekly totals. Deterministic dates (no clock reads).
 */
internal fun sampleDashboardData(): DashboardData {
    val monday = LocalDate.of(2026, 6, 1) // a fixed Monday for stable snapshots
    val today = monday.plusDays(2) // "Wednesday"
    val protocols = listOf(
        DerivedProtocol(
            id = "p1",
            label = "Testosterone",
            calculatorType = "trt",
            startDate = monday,
            freqDays = 3.5,
            doseLabel = "50.0 mg",
        ),
        DerivedProtocol(
            id = "p2",
            label = "Semaglutide",
            calculatorType = "semaglutide",
            startDate = monday,
            freqDays = 7.0,
            doseLabel = "0.5 mg",
        ),
        DerivedProtocol(
            id = "p3",
            label = "BPC-157",
            calculatorType = "bpc157",
            startDate = monday,
            freqDays = 1.0,
            doseLabel = "250.0 mcg",
        ),
    )
    val weekStrip = (0 until 7).map { i ->
        val date = monday.plusDays(i.toLong())
        DayDoses(
            date = date,
            isToday = date == today,
            doseCount = protocols.count { isDoseDayLocal(it, date) },
        )
    }
    return DashboardData(
        profile = UserProfile(id = "u1", displayName = "Pouroa Frew", email = "you@example.com"),
        greeting = "Good morning",
        dosages = emptyList(),
        protocols = protocols,
        activeCycle = null,
        nextDose = ProjectedDose(
            dosageId = "p1",
            date = today,
            label = "Testosterone",
            calculatorType = "trt",
            doseLabel = "50.0 mg",
        ),
        nextDoseCountdownDays = 0L,
        cycleDay = 3,
        cycleTotalDays = 84,
        weekStrip = weekStrip,
        weeklyTotals = listOf(
            "2× 50.0 mg · Testosterone",
            "1× 0.5 mg · Semaglutide",
            "7× 250.0 mcg · BPC-157",
        ),
    )
}

// Local mirror of the dose-day rule so the sample stays self-contained (matches the engine).
private fun isDoseDayLocal(p: DerivedProtocol, date: LocalDate): Boolean {
    val d = java.time.temporal.ChronoUnit.DAYS.between(p.startDate, date)
    if (d < 0) return false
    val f = p.freqDays
    if (f <= 0) return false
    return if (f == Math.floor(f)) d % f.toLong() == 0L
    else Math.round(Math.round(d / f) * f) == d
}
