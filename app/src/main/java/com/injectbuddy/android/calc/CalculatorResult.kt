package com.injectbuddy.android.calc

/**
 * Flat, presentation-ready output of any calculator. The generic screen renders this
 * verbatim, so the engine — not the UI — owns formatting decisions (units, ordering,
 * which secondary lines to show). Mirrors the web app's "primary draw + KPI rows" panel.
 *
 * [primaryValue]/[primaryUnit] is the headline figure (e.g. "0.25 mL"). [secondary] is
 * the ordered list of supporting lines (units, mg/inj, schedule, category band, etc.).
 * [isValid] gates display: false means inputs are incomplete/degenerate (web's `isValid`).
 */
data class CalculatorResult(
    val primaryValue: Double,
    val primaryUnit: String,
    val secondary: List<ResultLine> = emptyList(),
    val isValid: Boolean = true,
) {
    companion object {
        /** Inputs incomplete or maths undefined — matches web's `isValid === false` gate. */
        val invalid = CalculatorResult(primaryValue = 0.0, primaryUnit = "", isValid = false)
    }
}

/** One labelled supporting figure, e.g. label="Units (U-100)", value="25", unit="U". */
data class ResultLine(
    val label: String,
    val value: String,
    val unit: String = "",
)
