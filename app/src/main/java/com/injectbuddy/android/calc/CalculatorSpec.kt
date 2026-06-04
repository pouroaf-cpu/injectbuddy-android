package com.injectbuddy.android.calc

/**
 * Declarative description of one calculator: its fields, titles, the Supabase
 * `calculator_type` it shares with the web app, and a pointer at the engine maths.
 * The generic CalculatorScreen is driven *entirely* from this registry — adding a field
 * here adds it to the UI, the live config JSON, and the saved-dosage payload.
 *
 * `calculatorType` strings are copied verbatim from app.js `calculator_type:` POST bodies
 * so Android-saved rows interleave with web rows in the shared `saved_dosages` table.
 * (slug → calculatorType): trt-dose→trt, trt-microdose→microdose, trt-eod→eod, hcg→hcg,
 * peptide→peptide, reconstitution→reconstitution, semaglutide/tirzepatide/retatrutide as-is,
 * bpc-157→bpc157, bpc-157-tb500→bpc157blend, bmi→bmi, free-t-index→freetest,
 * cycle-plotter→plotter. BMI and free-t-index are local-only on the web (no save endpoint).
 */

enum class FieldType { Number, Dropdown, Toggle }

data class Option(val value: String, val label: String)

data class Field(
    val key: String,
    val label: String,
    val type: FieldType,
    val unit: String? = null,
    val default: String? = null,
    val options: List<Option> = emptyList(),
)

data class CalculatorSpec(
    val slug: String,
    val calculatorType: String,
    val title: String,
    val subtitle: String,
    val fields: List<Field>,
    /** True when the web exposes a Save-protocol endpoint for this calc (not BMI/freetest). */
    val savable: Boolean,
    val evaluate: (Map<String, Any?>) -> CalculatorResult,
)

private object CalcOptions {
    // app.js ESTER_TYPE_OPTIONS (~line 552)
    val esters = listOf(
        "Testosterone Cypionate", "Testosterone Enanthate", "Testosterone Propionate",
        "Testosterone Undecanoate", "Testosterone Acetate", "Testosterone Suspension",
        "Sustanon 250",
    ).map { Option(it, it) }

    // app.js TRT/microdose modes
    val trtModes = listOf(
        Option("ndays", "Every N days"),
        Option("perweek", "Injections per week"),
        Option("ml2mg", "mL drawn → mg"),
    )

    // app.js PEPTIDE_TYPES (~line 4355)
    val peptideTypes = listOf(
        "BPC-157", "TB-500 (Thymosin Beta-4)", "CJC-1295 (no DAC)", "CJC-1295 + DAC",
        "Ipamorelin", "GHRP-2", "GHRP-6", "Sermorelin", "HGH (Somatropin)",
        "PT-141 (Bremelanotide)", "IGF-1 LR3", "Epithalon", "Thymosin Alpha-1",
        "Selank", "Semax", "GHK-Cu", "Melanotan II", "Oxytocin",
        "Semaglutide", "Tirzepatide", "Retatrutide",
    ).map { Option(it, it) }

    val mgMcg = listOf(Option("mcg", "mcg"), Option("mg", "mg"))
    val unitsSystem = listOf(Option("metric", "Metric"), Option("imperial", "Imperial"))
    val ttUnits = listOf(Option("nmol", "nmol/L"), Option("ngdl", "ng/dL"))
    val vialMcg = listOf(Option("5000", "5 mg (5,000 mcg)"), Option("10000", "10 mg (10,000 mcg)"))
}

/** All non-plotter specs, keyed by slug. */
private val specs: Map<String, CalculatorSpec> = buildList {
    // ── TRT dose (calculator_type 'trt') ──────────────────────────────────────
    add(
        CalculatorSpec(
            slug = "trt-dose",
            calculatorType = "trt",
            title = "Testosterone Dosage Calculator",
            subtitle = "Weekly dose → mg, mL and units per injection",
            savable = true,
            fields = listOf(
                Field("esterType", "Ester type", FieldType.Dropdown, options = CalcOptions.esters),
                Field("mode", "Schedule mode", FieldType.Dropdown, default = "ndays", options = CalcOptions.trtModes),
                Field("strength", "Vial strength", FieldType.Number, unit = "mg/mL"),
                Field("mgWeek", "Weekly dose", FieldType.Number, unit = "mg/wk"),
                Field("nDays", "Inject every", FieldType.Number, unit = "days", default = "3.5"),
                Field("injPerWeek", "Injections per week", FieldType.Number, unit = "×"),
                Field("mlDrawn", "Volume drawn (mL→mg mode)", FieldType.Number, unit = "mL"),
            ),
            evaluate = { CalculatorEngine.evaluate("trt-dose", it) },
        ),
    )

    // ── TRT microdose (calculator_type 'microdose') — same maths as TRT ────────
    add(
        CalculatorSpec(
            slug = "trt-microdose",
            calculatorType = "microdose",
            title = "Testosterone Microdose Calculator",
            subtitle = "Low-dose protocols in 0.5 mg increments",
            savable = true,
            fields = listOf(
                Field("esterType", "Ester type", FieldType.Dropdown, options = CalcOptions.esters),
                Field("mode", "Schedule mode", FieldType.Dropdown, default = "ndays", options = CalcOptions.trtModes),
                Field("strength", "Vial strength", FieldType.Number, unit = "mg/mL"),
                Field("mgWeek", "Weekly dose", FieldType.Number, unit = "mg/wk"),
                Field("nDays", "Inject every", FieldType.Number, unit = "days", default = "3.5"),
                Field("injPerWeek", "Injections per week", FieldType.Number, unit = "×"),
                Field("mlDrawn", "Volume drawn (mL→mg mode)", FieldType.Number, unit = "mL"),
            ),
            evaluate = { CalculatorEngine.evaluate("trt-microdose", it) },
        ),
    )

    // ── EOD (calculator_type 'eod') ───────────────────────────────────────────
    add(
        CalculatorSpec(
            slug = "trt-eod",
            calculatorType = "eod",
            title = "EOD Testosterone Calculator",
            subtitle = "Every-other-day dosing (3.5 injections/week)",
            savable = true,
            fields = listOf(
                Field("esterType", "Ester type", FieldType.Dropdown, options = CalcOptions.esters),
                Field("strength", "Vial strength", FieldType.Number, unit = "mg/mL"),
                Field("mgWeek", "Weekly dose", FieldType.Number, unit = "mg/wk"),
            ),
            evaluate = { CalculatorEngine.evaluate("trt-eod", it) },
        ),
    )

    // ── HCG (calculator_type 'hcg') ───────────────────────────────────────────
    add(
        CalculatorSpec(
            slug = "hcg",
            calculatorType = "hcg",
            title = "HCG Dosage Calculator",
            subtitle = "Vial IU + BAC water → draw volume and units",
            savable = true,
            fields = listOf(
                Field("vialIU", "Vial strength", FieldType.Number, unit = "IU"),
                Field("bacWaterMl", "BAC water added", FieldType.Number, unit = "mL"),
                Field("dose", "Dose per injection", FieldType.Number, unit = "IU"),
            ),
            evaluate = { CalculatorEngine.evaluate("hcg", it) },
        ),
    )

    // ── Peptide (calculator_type 'peptide') ───────────────────────────────────
    add(
        CalculatorSpec(
            slug = "peptide",
            calculatorType = "peptide",
            title = "Peptide Dosage Calculator",
            subtitle = "Reconstituted peptide → draw volume and units",
            savable = true,
            fields = listOf(
                Field("peptideType", "Peptide type", FieldType.Dropdown, options = CalcOptions.peptideTypes),
                Field("peptideMg", "Peptide in vial", FieldType.Number, unit = "mg"),
                Field("bawMl", "BAC water added", FieldType.Number, unit = "mL", default = "2"),
                Field("dosePerInj", "Dose per injection", FieldType.Number),
                Field("doseUnit", "Dose unit", FieldType.Dropdown, default = "mcg", options = CalcOptions.mgMcg),
                Field("injPerWeek", "Injections per week", FieldType.Number, unit = "×", default = "7"),
            ),
            evaluate = { CalculatorEngine.evaluate("peptide", it) },
        ),
    )

    // ── Reconstitution (calculator_type 'reconstitution') ─────────────────────
    add(
        CalculatorSpec(
            slug = "reconstitution",
            calculatorType = "reconstitution",
            title = "Reconstitution Calculator",
            subtitle = "Solve BAC water for a target concentration",
            savable = true,
            fields = listOf(
                Field("peptideMg", "Peptide in vial", FieldType.Number, unit = "mg"),
                Field("targetConc", "Target concentration", FieldType.Number, unit = "mcg/mL"),
            ),
            evaluate = { CalculatorEngine.evaluate("reconstitution", it) },
        ),
    )

    // ── GLP-1 trio — semaglutide / tirzepatide / retatrutide ──────────────────
    add(glp1Spec("semaglutide", "semaglutide", "Semaglutide Dosage Calculator"))
    add(glp1Spec("tirzepatide", "tirzepatide", "Tirzepatide Dosage Calculator"))
    add(glp1Spec("retatrutide", "retatrutide", "Retatrutide Dosage Calculator"))

    // ── BPC-157 (calculator_type 'bpc157') ────────────────────────────────────
    add(
        CalculatorSpec(
            slug = "bpc-157",
            calculatorType = "bpc157",
            title = "BPC-157 Calculator",
            subtitle = "Vial concentration + dose (mcg) → draw volume",
            savable = true,
            fields = listOf(
                Field("vialMcg", "Vial size", FieldType.Dropdown, default = "5000", options = CalcOptions.vialMcg),
                Field("concMcgMl", "Concentration", FieldType.Number, unit = "mcg/mL"),
                Field("dose", "Dose per injection", FieldType.Number, unit = "mcg"),
            ),
            evaluate = { CalculatorEngine.evaluate("bpc-157", it) },
        ),
    )

    // ── BPC-157 + TB-500 blend (calculator_type 'bpc157blend') ────────────────
    add(
        CalculatorSpec(
            slug = "bpc-157-tb500",
            calculatorType = "bpc157blend",
            title = "BPC-157 + TB-500 Blend Calculator",
            subtitle = "Two peptides, one syringe — combined draw",
            savable = true,
            fields = listOf(
                Field("bpcVial", "BPC-157 vial size", FieldType.Dropdown, default = "5000", options = CalcOptions.vialMcg),
                Field("bpcWater", "BPC-157 BAC water", FieldType.Number, unit = "mL"),
                Field("bpcDose", "BPC-157 dose", FieldType.Number, unit = "mcg"),
                Field("tbVial", "TB-500 vial size", FieldType.Dropdown, default = "5000", options = CalcOptions.vialMcg),
                Field("tbWater", "TB-500 BAC water", FieldType.Number, unit = "mL"),
                Field("tbDose", "TB-500 dose", FieldType.Number, unit = "mcg"),
            ),
            evaluate = { CalculatorEngine.evaluate("bpc-157-tb500", it) },
        ),
    )

    // ── BMI (local-only on web — not savable) ─────────────────────────────────
    add(
        CalculatorSpec(
            slug = "bmi",
            calculatorType = "bmi",
            title = "BMI Calculator",
            subtitle = "Body Mass Index with WHO categories",
            savable = false,
            fields = listOf(
                Field("units", "Units", FieldType.Dropdown, default = "metric", options = CalcOptions.unitsSystem),
                Field("weightKg", "Weight (metric)", FieldType.Number, unit = "kg"),
                Field("heightCm", "Height (metric)", FieldType.Number, unit = "cm"),
                Field("weightLb", "Weight (imperial)", FieldType.Number, unit = "lb"),
                Field("heightFt", "Height feet (imperial)", FieldType.Number, unit = "ft"),
                Field("heightIn", "Height inches (imperial)", FieldType.Number, unit = "in"),
            ),
            evaluate = { CalculatorEngine.evaluate("bmi", it) },
        ),
    )

    // ── Free Testosterone Index / FAI (local-only on web — not savable) ───────
    add(
        CalculatorSpec(
            slug = "free-t-index",
            calculatorType = "freetest",
            title = "Free Testosterone Index",
            subtitle = "Free Androgen Index = Total T ÷ SHBG × 100",
            savable = false,
            fields = listOf(
                Field("ttUnit", "Total-T units", FieldType.Dropdown, default = "nmol", options = CalcOptions.ttUnits),
                Field("totalT", "Total testosterone", FieldType.Number),
                Field("shbg", "SHBG", FieldType.Number, unit = "nmol/L"),
            ),
            evaluate = { CalculatorEngine.evaluate("free-t-index", it) },
        ),
    )
}.associateBy { it.slug }

/** GLP-1 calculators share identical fields and maths; only labels/type differ. */
private fun glp1Spec(slug: String, calcType: String, title: String) = CalculatorSpec(
    slug = slug,
    calculatorType = calcType,
    title = title,
    subtitle = "Compounded vial · dose (mg) → draw volume · U-100 units",
    savable = true,
    fields = listOf(
        Field("conc", "Vial concentration", FieldType.Number, unit = "mg/mL"),
        Field("dose", "Dose", FieldType.Number, unit = "mg"),
    ),
    evaluate = { CalculatorEngine.evaluate(slug, it) },
)

/** The plotter slug is recognised by the registry but has no generic spec (custom screen). */
const val CYCLE_PLOTTER_SLUG = "cycle-plotter"

/** Registry lookup. Returns null for unknown slugs and for cycle-plotter (handled separately). */
fun specFor(slug: String): CalculatorSpec? = specs[slug]
