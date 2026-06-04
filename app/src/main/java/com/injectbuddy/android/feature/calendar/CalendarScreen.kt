@file:OptIn(ExperimentalMaterial3Api::class)

package com.injectbuddy.android.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.injectbuddy.android.core.UiState
import com.injectbuddy.android.di.ServiceLocator
import com.injectbuddy.android.ui.components.EmptyState
import com.injectbuddy.android.ui.components.ErrorState
import com.injectbuddy.android.ui.components.LoadingState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendar (SCREENS.md §5): a month grid with dose dots + a day agenda for the tapped day.
 * Projection is the pure [projectDosesByDay] engine (shared with the dashboard, unit-
 * tested). Owns its Scaffold + TopAppBar (menu → drawer). Empty state CTA mirrors the
 * dashboard's. Signature is fixed — AppNavHost calls CalendarScreen(openDrawer).
 */
@Composable
fun CalendarScreen(openDrawer: () -> Unit) {
    val vm: CalendarViewModel = viewModel {
        CalendarViewModel(ServiceLocator.accountRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            is UiState.Loading -> LoadingState(Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, onRetry = vm::load, modifier = Modifier.padding(padding))
            is UiState.Empty -> EmptyState(
                title = "Nothing scheduled",
                message = "Add a protocol to see your schedule.",
                actionLabel = "Open dashboard",
                onAction = openDrawer, // drawer is the route back to the dashboard / calculators
                modifier = Modifier.padding(padding),
            )
            is UiState.Content -> CalendarContent(
                data = s.data,
                contentPadding = padding,
                onPrevMonth = vm::previousMonth,
                onNextMonth = vm::nextMonth,
                onToday = vm::goToToday,
                onSelectDay = vm::selectDay,
            )
        }
    }
}

@Composable
private fun CalendarContent(
    data: CalendarData,
    contentPadding: PaddingValues,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonthHeader(
            month = data.visibleMonth,
            onPrev = onPrevMonth,
            onNext = onNextMonth,
            onToday = onToday,
        )
        WeekdayHeader()
        MonthGrid(
            month = data.visibleMonth,
            today = data.today,
            selectedDay = data.selectedDay,
            dosesByDay = data.dosesByDay,
            onSelectDay = onSelectDay,
        )
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        DayAgenda(day = data.selectedDay, doses = data.selectedDoses)
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val label = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"
        Text(label, style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            TextButton(onClick = onToday) { Text("Today") }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth()) {
        // Monday-first week, matching the wireframe (Mo Tu We Th Fr Sa Su).
        for (dow in MONDAY_FIRST) {
            Text(
                dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selectedDay: LocalDate,
    dosesByDay: Map<LocalDate, List<ProjectedDose>>,
    onSelectDay: (LocalDate) -> Unit,
) {
    val firstOfMonth = month.atDay(1)
    // Leading blanks so day 1 lands under the right weekday (Monday-first).
    val leadingBlanks = (firstOfMonth.dayOfWeek.value + 6) % 7
    val daysInMonth = month.lengthOfMonth()
    val cells = leadingBlanks + daysInMonth

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(count = cells, key = { it }) { index ->
            if (index < leadingBlanks) {
                Box(Modifier.aspectRatio(1f))
            } else {
                val date = month.atDay(index - leadingBlanks + 1)
                DayCell(
                    date = date,
                    isToday = date == today,
                    isSelected = date == selectedDay,
                    doseCount = dosesByDay[date]?.size ?: 0,
                    onClick = { onSelectDay(date) },
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    doseCount: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier,
            )
            .then(
                if (isToday) Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            // Up to three dose dots under the number.
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                repeat(doseCount.coerceAtMost(3)) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayAgenda(day: LocalDate, doses: List<ProjectedDose>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            day.format(AGENDA_FMT),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (doses.isEmpty()) {
            Text(
                "No doses scheduled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            doses.forEach { dose ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // TODO(dose-log): tap → optimistic "mark taken" once dose_history exists.
                        .clickable { }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(dose.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(dose.doseLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private val MONDAY_FIRST = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

private val AGENDA_FMT: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
