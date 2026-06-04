package com.injectbuddy.android.feature.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** One compound row in the plotter: name, weekly dose, and its active span (start/duration). */
private data class PlotterCompound(
    val id: Int,
    val name: String = "",
    val weeklyDose: String = "",
    val startWeek: String = "1",
    val durationWeeks: String = "12",
)

/**
 * Simplified cycle plotter — a timeline editor rather than the generic field form. The web
 * plotter (app.js PlotterPage) runs a full PK concentration simulation with half-lives and
 * Chart.js; this Android version keeps the core planning loop the screen is about: add/remove
 * compounds and see each compound's active weeks laid out as a Gantt-style week strip.
 *
 * Deliberately self-contained (no ViewModel / persistence) — it's a visual planning surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyclePlotterScreen(onBack: () -> Unit) {
    val compounds = remember {
        mutableStateListOf(PlotterCompound(id = 1, name = "Test E", weeklyDose = "250"))
    }
    var nextId by remember { mutableStateOf(2) }

    // Timeline length = furthest (startWeek + duration) across all compounds, min 12.
    val totalWeeks = remember(compounds.toList()) {
        val end = compounds.maxOfOrNull { c ->
            (c.startWeek.toIntOrNull() ?: 1) + (c.durationWeeks.toIntOrNull() ?: 0) - 1
        } ?: 12
        maxOf(end, 12)
    }

    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        Color(0xFF7C3AED),
        Color(0xFFF97316),
        Color(0xFF0EA5E9),
        Color(0xFFE11D48),
        Color(0xFF16A34A),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cycle Plotter") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Plan a cycle: add compounds with a weekly dose and an active span. The strip below shows each compound's active weeks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Compound editors ──────────────────────────────────────────────
            itemsIndexedCompat(compounds) { index, compound ->
                CompoundEditor(
                    compound = compound,
                    accent = palette[index % palette.size],
                    onChange = { updated -> compounds[index] = updated },
                    onRemove = if (compounds.size > 1) {
                        { compounds.removeAt(index) }
                    } else null,
                )
            }

            item {
                FilledTonalButton(
                    onClick = {
                        compounds.add(PlotterCompound(id = nextId))
                        nextId += 1
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add compound")
                }
            }

            // ── Gantt-style timeline ──────────────────────────────────────────
            item {
                Text(
                    "Timeline · $totalWeeks weeks",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                TimelineGantt(
                    compounds = compounds,
                    totalWeeks = totalWeeks,
                    palette = palette,
                )
            }
            item {
                Text(
                    "Maths only — not medical advice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CompoundEditor(
    compound: PlotterCompound,
    accent: Color,
    onChange: (PlotterCompound) -> Unit,
    onRemove: (() -> Unit)?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(accent))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = compound.name,
                    onValueChange = { onChange(compound.copy(name = it)) },
                    label = { Text("Compound") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                if (onRemove != null) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove compound")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = compound.weeklyDose,
                    onValueChange = { onChange(compound.copy(weeklyDose = it)) },
                    label = { Text("Dose/wk") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = compound.startWeek,
                    onValueChange = { onChange(compound.copy(startWeek = it)) },
                    label = { Text("Start wk") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = compound.durationWeeks,
                    onValueChange = { onChange(compound.copy(durationWeeks = it)) },
                    label = { Text("Weeks") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Horizontal week strip: a header row of week numbers + one active-span bar per compound. */
@Composable
private fun TimelineGantt(
    compounds: List<PlotterCompound>,
    totalWeeks: Int,
    palette: List<Color>,
) {
    val cellWidth = 30.dp
    val weeks = (1..totalWeeks).toList()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Week-number header.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                itemsCompat(weeks) { week ->
                    Box(
                        modifier = Modifier.width(cellWidth).height(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // One row per compound.
            compounds.forEachIndexed { index, compound ->
                val accent = palette[index % palette.size]
                val start = compound.startWeek.toIntOrNull() ?: 1
                val duration = compound.durationWeeks.toIntOrNull() ?: 0
                val endWeek = start + duration - 1
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(vertical = 3.dp),
                ) {
                    itemsCompat(weeks) { week ->
                        val active = duration > 0 && week in start..endWeek
                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .height(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .then(
                                    if (active) Modifier.background(accent)
                                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                                ),
                        )
                    }
                }
            }
        }
    }
}

// Small local helpers to avoid importing itemsIndexed/items extension ambiguity in LazyColumn vs LazyRow.
private inline fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsIndexedCompat(
    items: List<T>,
    crossinline itemContent: @Composable (index: Int, item: T) -> Unit,
) = items(items.size) { i -> itemContent(i, items[i]) }

private inline fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsCompat(
    items: List<T>,
    crossinline itemContent: @Composable (item: T) -> Unit,
) = items(items.size) { i -> itemContent(items[i]) }
