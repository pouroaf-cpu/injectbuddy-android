package com.injectbuddy.android.feature.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.injectbuddy.android.calc.CalculatorResult
import com.injectbuddy.android.calc.CalculatorSpec
import com.injectbuddy.android.calc.FieldType
import com.injectbuddy.android.calc.specFor
import com.injectbuddy.android.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/** UI-facing status of the "Save as protocol" action. */
sealed interface SaveStatus {
    data object Idle : SaveStatus
    data object Saving : SaveStatus
    data object Saved : SaveStatus
    data class Error(val message: String) : SaveStatus
}

/**
 * Holds live input state for one calculator and recomputes its [CalculatorResult] on every
 * keystroke via the pure [com.injectbuddy.android.calc.CalculatorEngine]. Save() persists the
 * current inputs as a `saved_dosages` row through [ServiceLocator.accountRepository], building
 * the config JSON from the spec's fields — same shape the web app posts.
 *
 * Reads [ServiceLocator] internally so the Compose factory stays a no-arg lambda.
 */
class CalculatorViewModel(private val slug: String) : ViewModel() {

    val spec: CalculatorSpec? = specFor(slug)

    /** Current raw input values keyed by field key (always strings; engine coerces). */
    private val _inputs = MutableStateFlow(initialInputs())
    val inputs: StateFlow<Map<String, String>> = _inputs.asStateFlow()

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    /** Live result, recomputed whenever any input changes. */
    val result: StateFlow<CalculatorResult> = _inputs
        .map { evaluate(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, evaluate(_inputs.value))

    private fun initialInputs(): Map<String, String> =
        spec?.fields?.associate { it.key to (it.default ?: "") } ?: emptyMap()

    private fun evaluate(values: Map<String, String>): CalculatorResult =
        spec?.evaluate?.invoke(values) ?: CalculatorResult.invalid

    fun onFieldChange(key: String, value: String) {
        _inputs.value = _inputs.value.toMutableMap().apply { put(key, value) }
        // Editing inputs invalidates a prior "Saved" badge, matching the web's save-signature reset.
        if (_saveStatus.value is SaveStatus.Saved) _saveStatus.value = SaveStatus.Idle
    }

    /** Build the JSON config the same way the web does: every field key → typed primitive. */
    private fun buildConfig(): JsonObject = buildJsonObject {
        val s = spec ?: return@buildJsonObject
        val values = _inputs.value
        for (field in s.fields) {
            val raw = values[field.key].orEmpty()
            when (field.type) {
                FieldType.Number -> put(field.key, JsonPrimitive(raw.trim().toDoubleOrNull() ?: 0.0))
                FieldType.Toggle -> put(field.key, JsonPrimitive(raw.toBoolean()))
                FieldType.Dropdown -> put(field.key, JsonPrimitive(raw))
            }
        }
    }

    fun save(label: String?) {
        val s = spec ?: return
        if (!s.savable) {
            _saveStatus.value = SaveStatus.Error("This calculator can't be saved.")
            return
        }
        if (!result.value.isValid) {
            _saveStatus.value = SaveStatus.Error("Complete the inputs first.")
            return
        }
        _saveStatus.value = SaveStatus.Saving
        viewModelScope.launch {
            val res = ServiceLocator.accountRepository.saveDosage(
                calculatorType = s.calculatorType,
                label = label?.ifBlank { null },
                config = buildConfig(),
            )
            _saveStatus.value = res.fold(
                onSuccess = { SaveStatus.Saved },
                onFailure = { SaveStatus.Error(it.message ?: "Save failed — try again") },
            )
        }
    }
}
