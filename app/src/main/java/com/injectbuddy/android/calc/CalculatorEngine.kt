package com.injectbuddy.android.calc

import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Stateless, pure-Kotlin port of the web calculator arithmetic (Injectbuddy/public/app.js).
 * Every formula here is copied verbatim from its React component so the Android app and the
 * web app produce identical numbers for the same inputs. NO Android imports — this file is
 * unit-testable on the JVM (see CalculatorEngineTest).
 *
 * Inputs arrive as a `Map<String, Any?>` keyed by the same field keys the [CalculatorSpec]
 * declares; helpers below coerce strings/numbers to Double and read the active dropdown/mode.
 * Each `evaluate*` returns a [CalculatorResult] mirroring the web result panel.
 */
object CalculatorEngine {

    // ── input coercion helpers ────────────────────────────────────────────────
    private fun Map<String, Any?>.num(key: String): Double = when (val v = this[key]) {
        is Number -> v.toDouble()
        is String -> v.trim().toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    private fun Map<String, Any?>.str(key: String): String = when (val v = this[key]) {
        is String -> v
        null -> ""
        else -> v.toString()
    }

    /**
     * Round to exactly [dp] decimals for display (e.g. 50.0 → "50.0", 2.0 @0dp → "2").
     * Deterministic — no trailing-zero trimming — so golden tests can pin exact strings.
     */
    private fun fmt(value: Double, dp: Int): String {
        if (!value.isFinite()) return "—"
        return if (dp == 0) {
            value.roundToLong().toString()
        } else {
            String.format(Locale.US, "%.${dp}f", value)
        }
    }

    // ── TRT / microdose shared core (app.js ~3591 & ~3797) ─────────────────────
    // Identical maths in TRTPage and the microdose page. Modes: 'ndays', 'perweek', 'ml2mg'.
    //   ndays:   freqPerWeek = 7/nDays;  mgPerInj = mgWeek/freqPerWeek; mlPerInj = mgPerInj/strength
    //   perweek: freqPerWeek = injPerWeek; (same mgPerInj/mlPerInj)
    //   ml2mg:   mlPerInj = mlDrawn; mgPerInj = mlDrawn*strength; weeklyTotal = mgPerInj*freqPerWeek
    //   unitsPerInj = mlPerInj * 100
    private fun evaluateTrtLike(i: Map<String, Any?>): CalculatorResult {
        val mode = i.str("mode").ifEmpty { "ndays" }
        val strength = i.num("strength")
        val mgWeek = i.num("mgWeek")
        val nDays = i.num("nDays")
        val injPerWeek = i.num("injPerWeek")
        val mlDrawn = i.num("mlDrawn")

        val mgPerInj: Double
        val mlPerInj: Double
        val freqPerWeek: Double
        val weeklyTotal: Double
        when (mode) {
            "perweek" -> {
                freqPerWeek = injPerWeek
                mgPerInj = mgWeek / freqPerWeek
                mlPerInj = mgPerInj / strength
                weeklyTotal = mgWeek
            }
            "ml2mg" -> {
                mlPerInj = mlDrawn
                mgPerInj = mlDrawn * strength
                freqPerWeek = injPerWeek
                weeklyTotal = mgPerInj * freqPerWeek
            }
            else -> { // 'ndays'
                freqPerWeek = 7.0 / nDays
                mgPerInj = mgWeek / freqPerWeek
                mlPerInj = mgPerInj / strength
                weeklyTotal = mgWeek
            }
        }
        val unitsPerInj = mlPerInj * 100.0
        val isValid = strength > 0 && mlPerInj > 0 && mlPerInj.isFinite()
        if (!isValid) return CalculatorResult.invalid
        return CalculatorResult(
            primaryValue = mlPerInj,
            primaryUnit = "mL",
            secondary = listOf(
                ResultLine("Units (U-100)", fmt(unitsPerInj, 0), "U"),
                ResultLine("Dose per injection", fmt(mgPerInj, 1), "mg"),
                ResultLine("Injections per week", fmt(freqPerWeek, 2), "×"),
                ResultLine("Weekly total", fmt(weeklyTotal, 1), "mg"),
            ),
            isValid = true,
        )
    }

    // ── EOD (app.js ~3961) — every-other-day, freqPerWeek fixed at 3.5 ─────────
    private fun evaluateEod(i: Map<String, Any?>): CalculatorResult {
        val strength = i.num("strength")
        val mgWeek = i.num("mgWeek")
        val freqPerWeek = 3.5
        val mgPerInj = mgWeek / freqPerWeek
        val mlPerInj = mgPerInj / strength
        val unitsPerInj = mlPerInj * 100.0
        val isValid = strength > 0 && mlPerInj > 0 && mlPerInj.isFinite()
        if (!isValid) return CalculatorResult.invalid
        return CalculatorResult(
            primaryValue = mlPerInj,
            primaryUnit = "mL",
            secondary = listOf(
                ResultLine("Units (U-100)", fmt(unitsPerInj, 0), "U"),
                ResultLine("Dose per injection", fmt(mgPerInj, 1), "mg"),
                ResultLine("Injections per week", fmt(freqPerWeek, 1), "×"),
                ResultLine("Weekly total", fmt(mgWeek, 1), "mg"),
            ),
        )
    }

    // ── HCG (app.js ~8715) ─────────────────────────────────────────────────────
    //   concentration = vialIU / bacWaterMl   (IU per mL)
    //   drawMl = dose / concentration ; units = round(drawMl*100)
    //   dosesPerVial = floor(vialIU / dose)
    private fun evaluateHcg(i: Map<String, Any?>): CalculatorResult {
        val vialIU = i.num("vialIU")
        val bacWaterMl = i.num("bacWaterMl")
        val dose = i.num("dose")
        val concentration = if (bacWaterMl > 0) vialIU / bacWaterMl else 0.0
        val drawMl = if (concentration > 0) dose / concentration else 0.0
        val units = (drawMl * 100.0).roundToInt()
        val dosesPerVial = if (dose > 0) floor(vialIU / dose).toInt() else 0
        val isValid = concentration > 0 && dose > 0 && drawMl.isFinite() && drawMl > 0
        if (!isValid) return CalculatorResult.invalid
        return CalculatorResult(
            primaryValue = drawMl,
            primaryUnit = "mL",
            secondary = listOf(
                ResultLine("Units (U-100)", units.toString(), "U"),
                ResultLine("Concentration", fmt(concentration, 0), "IU/mL"),
                ResultLine("Doses per vial", dosesPerVial.toString()),
            ),
        )
    }

    // ── Peptide (app.js ~4228) ─────────────────────────────────────────────────
    //   concentration = peptideMg / bawMl   (mg per mL)
    //   dosePerInjMg = (doseUnit=='mcg') ? dosePerInj/1000 : dosePerInj
    //   mlPerInj = dosePerInjMg / concentration ; unitsPerInj = mlPerInj*100
    //   totalDoses = peptideMg / dosePerInjMg
    private fun evaluatePeptide(i: Map<String, Any?>): CalculatorResult {
        val peptideMg = i.num("peptideMg")
        val bawMl = i.num("bawMl")
        val dosePerInj = i.num("dosePerInj")
        val doseUnit = i.str("doseUnit").ifEmpty { "mcg" }
        val concentration = if (bawMl > 0) peptideMg / bawMl else 0.0
        val dosePerInjMg = if (doseUnit == "mcg") dosePerInj / 1000.0 else dosePerInj
        val mlPerInj = if (concentration > 0 && dosePerInjMg > 0) dosePerInjMg / concentration else 0.0
        val unitsPerInj = mlPerInj * 100.0
        val totalDoses = if (dosePerInjMg > 0) peptideMg / dosePerInjMg else 0.0
        val isValid = concentration > 0 && dosePerInjMg > 0 && mlPerInj.isFinite() && mlPerInj > 0
        if (!isValid) return CalculatorResult.invalid
        return CalculatorResult(
            primaryValue = mlPerInj,
            primaryUnit = "mL",
            secondary = listOf(
                ResultLine("Units (U-100)", fmt(unitsPerInj, 0), "U"),
                ResultLine("Concentration", fmt(concentration, 2), "mg/mL"),
                ResultLine("Doses per vial", fmt(totalDoses, 0)),
            ),
        )
    }

    // ── Reconstitution (app.js ~5959) ──────────────────────────────────────────
    //   bacWaterMl = (peptideMg * 1000) / targetConc   (targetConc is mcg/mL)
    //   concentration after add = targetConc mcg/mL
    private fun evaluateReconstitution(i: Map<String, Any?>): CalculatorResult {
        val peptideMg = i.num("peptideMg")
        val targetConc = i.num("targetConc")
        val bacWaterMl = if (targetConc > 0) (peptideMg * 1000.0) / targetConc else 0.0
        val vialContentsMcg = peptideMg * 1000.0
        val isValid = peptideMg > 0 && targetConc > 0 && bacWaterMl.isFinite() && bacWaterMl > 0
        if (!isValid) return CalculatorResult.invalid
        return CalculatorResult(
            primaryValue = bacWaterMl,
            primaryUnit = "mL",
            secondary = listOf(
                ResultLine("Target concentration", fmt(targetConc, 0), "mcg/mL"),
                ResultLine("Vial contents", fmt(vialContentsMcg, 0), "mcg"),
            ),
        )
    }

    // ── GLP-1 (semaglutide / tirzepatide / retatrutide — app.js ~8123/8234/8344)
    //   volumeMl = dose / conc ; units = round(volumeMl*100). All three identical.
    private fun evaluateGlp1(i: Map<String, Any?>): CalculatorResult {
        val conc = i.num("conc")
        val dose = i.num("dose")
        val volumeMl = if (conc > 0 && dose > 0) dose / conc else 0.0
        val units = (volumeMl * 100.0).roundToInt()
        val isValid = conc > 0 && dose > 0 && volumeMl.isFinite() && volumeMl > 0
        if (!isValid) return CalculatorResult.invalid
        return CalculatorResult(
            primaryValue = volumeMl,
            primaryUnit = "mL",
            secondary = listOf(
                ResultLine("Units (U-100)", units.toString(), "U"),
                ResultLine("Concentration", fmt(conc, 1), "mg/mL"),
            ),
        )
    }

    // ── BPC-157 (app.js ~8465) ─────────────────────────────────────────────────
    //   conc = concMcgMl (mcg/mL) ; dose in mcg
    //   drawMl = dose / conc ; units = round(drawMl*100)
    private fun evaluateBpc157(i: Map<String, Any?>): CalculatorResult {
        val conc = i.num("concMcgMl")
        val dose = i.num("dose")
        val drawMl = if (conc > 0 && dose > 0) dose / conc else 0.0
        val units = (drawMl * 100.0).roundToInt()
        val isValid = conc > 0 && dose > 0 && drawMl.isFinite() && drawMl > 0
        if (!isValid) return CalculatorResult.invalid
        return CalculatorResult(
            primaryValue = drawMl,
            primaryUnit = "mL",
            secondary = listOf(
                ResultLine("Units (U-100)", units.toString(), "U"),
                ResultLine("Concentration", fmt(conc, 0), "mcg/mL"),
            ),
        )
    }

    // ── BPC-157 + TB-500 blend (app.js ~8594) ──────────────────────────────────
    //   bpcConc = bpcVial(mcg) / bpcWater(mL) ; bpcDraw = bpcDose(mcg) / bpcConc
    //   tbConc  = tbVial(mcg)  / tbWater(mL)  ; tbDraw  = tbDose(mcg)  / tbConc
    //   totalMl = bpcDraw + tbDraw ; units rounded per-peptide then summed
    private fun evaluateBpc157Blend(i: Map<String, Any?>): CalculatorResult {
        val bpcVial = i.num("bpcVial")
        val bpcWater = i.num("bpcWater")
        val bpcDose = i.num("bpcDose")
        val tbVial = i.num("tbVial")
        val tbWater = i.num("tbWater")
        val tbDose = i.num("tbDose")
        val bpcConc = if (bpcWater > 0) bpcVial / bpcWater else 0.0
        val tbConc = if (tbWater > 0) tbVial / tbWater else 0.0
        val bpcDraw = if (bpcConc > 0 && bpcDose > 0) bpcDose / bpcConc else 0.0
        val tbDraw = if (tbConc > 0 && tbDose > 0) tbDose / tbConc else 0.0
        val bpcUnits = (bpcDraw * 100.0).roundToInt()
        val tbUnits = (tbDraw * 100.0).roundToInt()
        val totalMl = bpcDraw + tbDraw
        val totalUnits = bpcUnits + tbUnits
        val bpcValid = bpcConc > 0 && bpcDose > 0 && bpcDraw.isFinite() && bpcDraw > 0
        val tbValid = tbConc > 0 && tbDose > 0 && tbDraw.isFinite() && tbDraw > 0
        if (!(bpcValid && tbValid)) return CalculatorResult.invalid
        return CalculatorResult(
            primaryValue = totalMl,
            primaryUnit = "mL",
            secondary = listOf(
                ResultLine("Combined units", totalUnits.toString(), "U"),
                ResultLine("BPC-157 draw", fmt(bpcDraw, 3), "mL"),
                ResultLine("TB-500 draw", fmt(tbDraw, 3), "mL"),
            ),
        )
    }

    // ── BMI (app.js ~6263) ──────────────────────────────────────────────────────
    //   metric:   bmi = weightKg / (heightCm/100)^2
    //   imperial: bmi = 703 * weightLb / (heightFt*12 + heightIn)^2
    //   WHO categories per BMI_CATS.
    private val bmiCats = listOf(
        Triple(0.0, 18.5, "Underweight"),
        Triple(18.5, 25.0, "Normal range"),
        Triple(25.0, 30.0, "Overweight"),
        Triple(30.0, 35.0, "Obesity I"),
        Triple(35.0, 40.0, "Obesity II"),
        Triple(40.0, 99.0, "Obesity III"),
    )

    private fun bmiCategory(bmi: Double): String =
        bmiCats.firstOrNull { bmi >= it.first && bmi < it.second }?.third ?: bmiCats.last().third

    private fun evaluateBmi(i: Map<String, Any?>): CalculatorResult {
        val units = i.str("units").ifEmpty { "metric" }
        val bmi: Double = if (units == "metric") {
            val hm = i.num("heightCm") / 100.0
            i.num("weightKg") / (hm * hm)
        } else {
            val totalIn = i.num("heightFt") * 12.0 + i.num("heightIn")
            703.0 * i.num("weightLb") / (totalIn * totalIn)
        }
        val isValid = bmi.isFinite() && bmi > 0
        if (!isValid) return CalculatorResult.invalid
        return CalculatorResult(
            primaryValue = bmi,
            primaryUnit = "",
            secondary = listOf(ResultLine("Category", bmiCategory(bmi))),
        )
    }

    // ── Free Testosterone Index / FAI (app.js ~6548) ───────────────────────────
    //   ttNmol = (unit=='ngdl') ? totalT/28.84 : totalT   (ng/dL → nmol/L)
    //   fai = (ttNmol / shbg) * 100
    //   bands: <30 Low · >150 Elevated · else Normal range (adult men)
    private fun evaluateFreeTest(i: Map<String, Any?>): CalculatorResult {
        val ttUnit = i.str("ttUnit").ifEmpty { "nmol" }
        val totalT = i.num("totalT")
        val shbg = i.num("shbg")
        val ttNmol = if (ttUnit == "ngdl") totalT / 28.84 else totalT
        val fai = if (ttNmol.isFinite() && shbg > 0) (ttNmol / shbg) * 100.0 else Double.NaN
        if (!fai.isFinite()) return CalculatorResult.invalid
        val band = when {
            fai < 30 -> "Low"
            fai > 150 -> "Elevated"
            else -> "Normal range (adult men)"
        }
        return CalculatorResult(
            primaryValue = fai,
            primaryUnit = "",
            secondary = listOf(ResultLine("Band", band)),
        )
    }

    /**
     * Single entry point. [slug] is the public calculator slug (see [CalculatorSpec]).
     * cycle-plotter is intentionally not handled here — it has its own timeline screen.
     */
    fun evaluate(slug: String, inputs: Map<String, Any?>): CalculatorResult = when (slug) {
        "trt-dose", "trt-microdose" -> evaluateTrtLike(inputs)
        "trt-eod" -> evaluateEod(inputs)
        "hcg" -> evaluateHcg(inputs)
        "peptide" -> evaluatePeptide(inputs)
        "reconstitution" -> evaluateReconstitution(inputs)
        "semaglutide", "tirzepatide", "retatrutide" -> evaluateGlp1(inputs)
        "bpc-157" -> evaluateBpc157(inputs)
        "bpc-157-tb500" -> evaluateBpc157Blend(inputs)
        "bmi" -> evaluateBmi(inputs)
        "free-t-index" -> evaluateFreeTest(inputs)
        else -> CalculatorResult.invalid
    }
}
