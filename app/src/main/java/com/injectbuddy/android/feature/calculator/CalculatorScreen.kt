package com.injectbuddy.android.feature.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
// ExposedDropdownMenu is a member of ExposedDropdownMenuBoxScope (not importable);
// it is called unqualified inside the ExposedDropdownMenuBox content below.
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.injectbuddy.android.calc.CYCLE_PLOTTER_SLUG
import com.injectbuddy.android.calc.CalculatorResult
import com.injectbuddy.android.calc.CalculatorSpec
import com.injectbuddy.android.calc.Field
import com.injectbuddy.android.calc.FieldType
import com.injectbuddy.android.calc.ResultLine
import com.injectbuddy.android.calc.specFor
import com.injectbuddy.android.ui.components.ErrorState

/**
 * Generic, spec-driven calculator screen. The form (LazyColumn of fields), the live result
 * card, and the save action are all derived from [com.injectbuddy.android.calc.CalculatorSpec],
 * so a new calculator needs no new UI. cycle-plotter is delegated to its bespoke timeline
 * screen; an unknown slug falls back to [ErrorState].
 *
 * Signature is fixed by the NavHost — do not change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(slug: String, onBack: () -> Unit) {
    if (slug == CYCLE_PLOTTER_SLUG) {
        CyclePlotterScreen(onBack = onBack)
        return
    }

    val vm: CalculatorViewModel = viewModel(key = slug) { CalculatorViewModel(slug) }
    val spec = vm.spec

    if (spec == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Calculator") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { padding ->
            ErrorState(
                message = "Unknown calculator \"$slug\".",
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    val inputs by vm.inputs.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val saveStatus by vm.saveStatus.collectAsStateWithLifecycle()

    CalculatorContent(
        spec = spec,
        inputs = inputs,
        result = result,
        saveStatus = saveStatus,
        onInput = vm::onFieldChange,
        onSave = { vm.save(label = null) },
        onBack = onBack,
    )
}

/**
 * Stateless body of [CalculatorScreen]: the whole screen UI (Scaffold + TopAppBar + the
 * spec-driven inputs LazyColumn + pinned result/save card) rendered purely from its
 * parameters. Holds no ViewModel/ServiceLocator/network state, so it can be driven by sample
 * data in a Paparazzi snapshot or a preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalculatorContent(
    spec: CalculatorSpec,
    inputs: Map<String, String>,
    result: CalculatorResult,
    saveStatus: SaveStatus,
    onInput: (key: String, value: String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(spec.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            ResultBar(
                result = result,
                saveStatus = saveStatus,
                savable = spec.savable,
                onSave = onSave,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(spec.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(spec.fields, key = { it.key }) { field ->
                CalcField(
                    field = field,
                    value = inputs[field.key].orEmpty(),
                    onChange = { onInput(field.key, it) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalcField(field: Field, value: String, onChange: (String) -> Unit) {
    val labelText = field.label + (field.unit?.let { " ($it)" } ?: "")
    when (field.type) {
        FieldType.Number -> OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(labelText) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        FieldType.Dropdown -> {
            var expanded by remember { mutableStateOf(false) }
            val selectedLabel = field.options.firstOrNull { it.value == value }?.label ?: ""
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(labelText) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    field.options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onChange(option.value)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        FieldType.Toggle -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(labelText, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = value.toBoolean(), onCheckedChange = { onChange(it.toString()) })
        }
    }
}

/** Pinned result + save card. Formats the engine's primary value and secondary lines. */
@Composable
private fun ResultBar(
    result: CalculatorResult,
    saveStatus: SaveStatus,
    savable: Boolean,
    onSave: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (result.isValid) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text("Result", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatPrimary(result),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (result.secondary.isNotEmpty()) {
                    HorizontalDivider()
                    result.secondary.forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(line.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                buildString {
                                    append(line.value)
                                    if (line.unit.isNotEmpty()) append(" ").append(line.unit)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Enter values to calculate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (savable) {
                Button(
                    onClick = onSave,
                    enabled = result.isValid && saveStatus != SaveStatus.Saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    when (saveStatus) {
                        SaveStatus.Saving -> CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        SaveStatus.Saved -> Text("Saved ✓")
                        else -> Text("Save as protocol")
                    }
                }
                (saveStatus as? SaveStatus.Error)?.let {
                    Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            Text(
                "Maths only — not medical advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatPrimary(result: CalculatorResult): String {
    // BMI / FAI have no unit and want one decimal; volumes want three.
    val value = if (result.primaryUnit.isEmpty()) {
        String.format(java.util.Locale.US, "%.1f", result.primaryValue)
    } else {
        String.format(java.util.Locale.US, "%.3f", result.primaryValue)
    }
    return if (result.primaryUnit.isEmpty()) value else "$value ${result.primaryUnit}"
}

// ── Sample data for snapshots / previews ──────────────────────────────────────
// Stateless realistic states so CalculatorContent can be rendered headlessly without a
// ViewModel, ServiceLocator, or engine call. Values mirror a real computed result.

/** Realistic TRT state: 200 mg/mL vial, 100 mg/wk, E3.5D → 0.25 mL / 25 U per injection. */
internal fun sampleTrtState(): CalculatorSampleState {
    val spec = specFor("trt-dose")!!
    val inputs = mapOf(
        "esterType" to "Testosterone Enanthate",
        "mode" to "ndays",
        "strength" to "200",
        "mgWeek" to "100",
        "nDays" to "3.5",
        "injPerWeek" to "",
        "mlDrawn" to "",
    )
    val result = CalculatorResult(
        primaryValue = 0.25,
        primaryUnit = "mL",
        secondary = listOf(
            ResultLine("Units (U-100)", "25", "U"),
            ResultLine("Dose per injection", "50", "mg"),
            ResultLine("Schedule", "every 3.5 days"),
            ResultLine("Injections per week", "2"),
        ),
    )
    return CalculatorSampleState(spec, inputs, result)
}

/** Realistic reconstitution-style peptide state (dropdowns + number fields, with a result). */
internal fun samplePeptideState(): CalculatorSampleState {
    val spec = specFor("peptide")!!
    val inputs = mapOf(
        "peptideType" to "BPC-157",
        "peptideMg" to "5",
        "bawMl" to "2",
        "dosePerInj" to "250",
        "doseUnit" to "mcg",
        "injPerWeek" to "7",
    )
    val result = CalculatorResult(
        primaryValue = 0.1,
        primaryUnit = "mL",
        secondary = listOf(
            ResultLine("Units (U-100)", "10", "U"),
            ResultLine("Concentration", "2500", "mcg/mL"),
            ResultLine("Doses per vial", "20"),
        ),
    )
    return CalculatorSampleState(spec, inputs, result)
}

/** Bundle of the three params [CalculatorContent] needs, so one factory feeds a snapshot. */
internal data class CalculatorSampleState(
    val spec: CalculatorSpec,
    val inputs: Map<String, String>,
    val result: CalculatorResult,
)
