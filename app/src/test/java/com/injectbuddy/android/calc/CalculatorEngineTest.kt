package com.injectbuddy.android.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden tests for [CalculatorEngine]. Expected values are derived from the web app formulas
 * in Injectbuddy/public/app.js — if these drift, the Android and web apps would disagree on a
 * dose, so each assertion pins one calculator to a hand-computed web value.
 */
class CalculatorEngineTest {

    private val delta = 1e-6

    /** Pull a labelled secondary line's value as a Double (units stripped). */
    private fun CalculatorResult.line(label: String): String =
        secondary.first { it.label == label }.value

    // ── TRT (calculator_type 'trt') — app.js ~3591 ─────────────────────────────
    // strength 200 mg/mL, 100 mg/wk, every 3.5 days → freq 2, mgPerInj 50, mlPerInj 0.25, units 25.
    @Test
    fun trtDose_ndays() {
        val r = CalculatorEngine.evaluate(
            "trt-dose",
            mapOf("mode" to "ndays", "strength" to "200", "mgWeek" to "100", "nDays" to "3.5"),
        )
        assertTrue(r.isValid)
        assertEquals(0.25, r.primaryValue, delta)
        assertEquals("mL", r.primaryUnit)
        assertEquals("25", r.line("Units (U-100)"))
        assertEquals("50.0", r.line("Dose per injection"))
        assertEquals("2.00", r.line("Injections per week"))
    }

    @Test
    fun trtDose_perweek() {
        // 100 mg/wk over 2 injections → 50 mg/inj, 0.25 mL.
        val r = CalculatorEngine.evaluate(
            "trt-dose",
            mapOf("mode" to "perweek", "strength" to "200", "mgWeek" to "100", "injPerWeek" to "2"),
        )
        assertEquals(0.25, r.primaryValue, delta)
    }

    @Test
    fun trtDose_ml2mg() {
        // mlDrawn 0.5 * strength 200 = 100 mg/inj; mlPerInj = 0.5.
        val r = CalculatorEngine.evaluate(
            "trt-dose",
            mapOf("mode" to "ml2mg", "strength" to "200", "mlDrawn" to "0.5", "injPerWeek" to "2"),
        )
        assertEquals(0.5, r.primaryValue, delta)
        assertEquals("100.0", r.line("Dose per injection"))
    }

    // ── Microdose ('microdose') — same maths as TRT, app.js ~3797 ──────────────
    @Test
    fun microdose_matchesTrt() {
        val r = CalculatorEngine.evaluate(
            "trt-microdose",
            mapOf("mode" to "ndays", "strength" to "20", "mgWeek" to "5", "nDays" to "3.5"),
        )
        // freq 2 → 2.5 mg/inj → 0.125 mL → 12.5 units.
        assertEquals(0.125, r.primaryValue, delta)
    }

    // ── EOD ('eod') — app.js ~3961, freq fixed 3.5 ─────────────────────────────
    @Test
    fun eod() {
        val r = CalculatorEngine.evaluate("trt-eod", mapOf("strength" to "200", "mgWeek" to "100"))
        assertEquals(0.14285714285714288, r.primaryValue, delta)
        assertEquals("14", r.line("Units (U-100)"))
    }

    // ── HCG ('hcg') — app.js ~8715 ─────────────────────────────────────────────
    @Test
    fun hcg() {
        val r = CalculatorEngine.evaluate(
            "hcg",
            mapOf("vialIU" to "5000", "bacWaterMl" to "2", "dose" to "250"),
        )
        assertEquals(0.1, r.primaryValue, delta) // 250 / (5000/2)
        assertEquals("10", r.line("Units (U-100)"))
        assertEquals("2500", r.line("Concentration"))
        assertEquals("20", r.line("Doses per vial")) // floor(5000/250)
    }

    // ── Peptide ('peptide') — app.js ~4228 ─────────────────────────────────────
    @Test
    fun peptide_mcg() {
        val r = CalculatorEngine.evaluate(
            "peptide",
            mapOf(
                "peptideMg" to "5", "bawMl" to "2", "dosePerInj" to "250",
                "doseUnit" to "mcg", "injPerWeek" to "7",
            ),
        )
        // conc 2.5 mg/mL, dose 0.25 mg → 0.1 mL → 10 units; 20 doses/vial.
        assertEquals(0.1, r.primaryValue, delta)
        assertEquals("10", r.line("Units (U-100)"))
        assertEquals("20", r.line("Doses per vial"))
    }

    // ── Reconstitution ('reconstitution') — app.js ~5959 ───────────────────────
    @Test
    fun reconstitution() {
        val r = CalculatorEngine.evaluate(
            "reconstitution",
            mapOf("peptideMg" to "5", "targetConc" to "1000"),
        )
        // (5 * 1000) / 1000 = 5 mL.
        assertEquals(5.0, r.primaryValue, delta)
    }

    // ── GLP-1 trio ('semaglutide'/'tirzepatide'/'retatrutide') — app.js 8123/8234/8344
    @Test
    fun semaglutide() {
        val r = CalculatorEngine.evaluate("semaglutide", mapOf("conc" to "5", "dose" to "0.5"))
        assertEquals(0.1, r.primaryValue, delta)
        assertEquals("10", r.line("Units (U-100)"))
    }

    @Test
    fun tirzepatide() {
        val r = CalculatorEngine.evaluate("tirzepatide", mapOf("conc" to "10", "dose" to "5"))
        assertEquals(0.5, r.primaryValue, delta)
        assertEquals("50", r.line("Units (U-100)"))
    }

    @Test
    fun retatrutide() {
        val r = CalculatorEngine.evaluate("retatrutide", mapOf("conc" to "4", "dose" to "2"))
        assertEquals(0.5, r.primaryValue, delta)
    }

    // ── BPC-157 ('bpc157') — app.js ~8465 ──────────────────────────────────────
    @Test
    fun bpc157() {
        val r = CalculatorEngine.evaluate("bpc-157", mapOf("concMcgMl" to "1000", "dose" to "250"))
        assertEquals(0.25, r.primaryValue, delta)
        assertEquals("25", r.line("Units (U-100)"))
    }

    // ── BPC-157 + TB-500 blend ('bpc157blend') — app.js ~8594 ──────────────────
    @Test
    fun bpc157Blend() {
        val r = CalculatorEngine.evaluate(
            "bpc-157-tb500",
            mapOf(
                "bpcVial" to "5000", "bpcWater" to "2", "bpcDose" to "250",
                "tbVial" to "5000", "tbWater" to "2.5", "tbDose" to "200",
            ),
        )
        // bpcConc 2500 → 0.1 mL; tbConc 2000 → 0.1 mL; total 0.2 mL, 20 combined units.
        assertEquals(0.2, r.primaryValue, delta)
        assertEquals("20", r.line("Combined units"))
    }

    // ── BMI ('bmi') — app.js ~6263 ─────────────────────────────────────────────
    @Test
    fun bmi_metric() {
        val r = CalculatorEngine.evaluate(
            "bmi",
            mapOf("units" to "metric", "weightKg" to "80", "heightCm" to "178"),
        )
        assertEquals(25.24933720489837, r.primaryValue, delta)
        assertEquals("Overweight", r.line("Category"))
    }

    @Test
    fun bmi_imperial() {
        val r = CalculatorEngine.evaluate(
            "bmi",
            mapOf("units" to "imperial", "weightLb" to "180", "heightFt" to "5", "heightIn" to "10"),
        )
        // 703 * 180 / 70^2.
        assertEquals(25.824489795918367, r.primaryValue, delta)
    }

    // ── Free Testosterone Index / FAI ('freetest') — app.js ~6548 ──────────────
    @Test
    fun freeTest_nmol() {
        val r = CalculatorEngine.evaluate(
            "free-t-index",
            mapOf("ttUnit" to "nmol", "totalT" to "20", "shbg" to "30"),
        )
        assertEquals(66.66666666666666, r.primaryValue, delta)
        assertEquals("Normal range (adult men)", r.line("Band"))
    }

    @Test
    fun freeTest_ngdl_convertsUnits() {
        val r = CalculatorEngine.evaluate(
            "free-t-index",
            mapOf("ttUnit" to "ngdl", "totalT" to "600", "shbg" to "30"),
        )
        // (600 / 28.84) / 30 * 100.
        assertEquals(69.34812760055479, r.primaryValue, delta)
    }

    @Test
    fun freeTest_lowBand() {
        val r = CalculatorEngine.evaluate(
            "free-t-index",
            mapOf("ttUnit" to "nmol", "totalT" to "5", "shbg" to "50"),
        )
        assertEquals(10.0, r.primaryValue, delta) // 5/50*100
        assertEquals("Low", r.line("Band"))
    }

    // ── Invalid / degenerate inputs gate to isValid = false ────────────────────
    @Test
    fun invalidWhenStrengthZero() {
        val r = CalculatorEngine.evaluate(
            "trt-dose",
            mapOf("mode" to "ndays", "strength" to "0", "mgWeek" to "100", "nDays" to "3.5"),
        )
        assertFalse(r.isValid)
    }

    @Test
    fun unknownSlugIsInvalid() {
        assertFalse(CalculatorEngine.evaluate("not-a-calc", emptyMap()).isValid)
    }
}
