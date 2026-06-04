package com.injectbuddy.android.feature.calendar

import com.injectbuddy.android.data.model.SavedDosage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.math.roundToLong

/**
 * Pure dose-projection engine — the Kotlin twin of the web's lib/account-schedule.ts.
 * No Android / no Supabase / no clock reads: `today` (or the window start) is always
 * passed in so this is deterministic and trivially unit-testable. The calendar UI and
 * the dashboard's next-dose both build on these functions.
 *
 * Frequency lives in each row's `config` and differs per calculator type (TRT uses
 * nDays, peptides use injPerWeek, etc.); we derive an interval in days exactly as the
 * web does. Fractional intervals (E3.5D, peptide 2.33d) are supported.
 */

/** One projected dose on a given day, for one saved protocol. */
data class ProjectedDose(
    val dosageId: String,
    val date: LocalDate,
    val label: String,
    val calculatorType: String,
    val doseLabel: String,
)

/** A protocol reduced to the mechanics the schedule needs (mirrors DerivedProtocol). */
data class DerivedProtocol(
    val id: String,
    val label: String,
    val calculatorType: String,
    val startDate: LocalDate,
    val freqDays: Double,
    val doseLabel: String,
)

/** Calc types with no injection schedule — never projected (parity with the web). */
private val NON_SCHEDULABLE = setOf("reconstitution", "bmi", "freetest", "free-t-index")

private fun JsonObject.num(key: String): Double {
    val v = this[key] as? JsonPrimitive ?: return Double.NaN
    return v.floatOrNull?.toDouble() ?: v.content.toDoubleOrNull() ?: Double.NaN
}

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.content

private fun Double.isNum() = !isNaN() && !isInfinite()
private fun round1(n: Double) = Math.round(n * 10.0) / 10.0

/** Parse a 'YYYY-MM-DD' Postgres date as a local calendar date (no UTC shift). */
fun parseLocalDate(s: String?): LocalDate? {
    if (s.isNullOrBlank()) return null
    // Accept a leading date even if a time/zone is appended (timestamptz columns).
    val datePart = s.take(10)
    return try {
        LocalDate.parse(datePart)
    } catch (_: DateTimeParseException) {
        null
    }
}

/**
 * Derive the dose interval (days) + a human dose label for a calc type from its config.
 * Returns null when the type isn't an injectable schedule. Mirrors deriveDose() in the
 * web engine — only the fields needed for projection (freqDays + doseLabel) are kept.
 */
private fun deriveDose(calc: String, cfg: JsonObject): Pair<Double, String>? {
    when (calc) {
        // TRT family. The web maps slugs trt/eod; the native slugs are trt-dose / trt-eod
        // / trt-microdose — all weekly-mg → per-injection on an nDays interval.
        "trt", "eod", "trt-dose", "trt-eod", "trt-microdose" -> {
            val mgWeek = cfg.num("mgWeek")
            val nDays = cfg.num("nDays")
            val freqDays = if (calc == "eod" || calc == "trt-eod") {
                2.0
            } else if (nDays.isNum() && nDays > 0) {
                nDays
            } else {
                7.0
            }
            val perInj = if (mgWeek.isNum()) round1(mgWeek * (freqDays / 7.0)) else Double.NaN
            val label = if (perInj.isNum()) "$perInj mg" else "— mg"
            return freqDays to label
        }

        "peptide" -> {
            val dosePerInj = cfg.num("dosePerInj")
            val doseUnit = cfg.str("doseUnit") ?: "mcg"
            val injPerWeek = cfg.num("injPerWeek").let { if (it.isNum() && it > 0) it else 7.0 }
            val freqDays = round2(7.0 / injPerWeek)
            val label = if (dosePerInj.isNum()) "$dosePerInj $doseUnit" else "— mcg"
            return freqDays to label
        }

        "semaglutide", "tirzepatide", "retatrutide" -> {
            val dose = cfg.num("dose")
            val label = if (dose.isNum()) "$dose mg" else "— mg"
            return 7.0 to label
        }

        "bpc157", "bpc-157" -> {
            val dose = cfg.num("dose")
            val label = if (dose.isNum()) "$dose mcg" else "— mcg"
            return 1.0 to label
        }

        "bpc157blend", "blend", "bpc-157-tb500" -> {
            val bpcDose = cfg.num("bpcDose")
            val tbDose = cfg.num("tbDose")
            val label = if (bpcDose.isNum() && tbDose.isNum()) "$bpcDose/$tbDose mcg" else "— mcg"
            return 1.0 to label
        }

        "hcg" -> {
            val dose = cfg.num("dose")
            val label = if (dose.isNum()) "$dose IU" else "— IU"
            return 3.5 to label
        }

        else -> return null
    }
}

private fun round2(n: Double) = Math.round(n * 100.0) / 100.0

/**
 * Reduce active, schedulable saved rows to their projection mechanics. Filters exactly
 * like the web's deriveProtocols(): inactive rows, non-injectable types, and rows with a
 * missing/invalid start_date are dropped (date math on a bad start would be a real
 * correctness hazard on a dose tool).
 */
fun deriveProtocols(rows: List<SavedDosage>): List<DerivedProtocol> =
    rows.mapNotNull { row ->
        if (!row.isActive) return@mapNotNull null
        if (row.calculatorType in NON_SCHEDULABLE) return@mapNotNull null
        val start = parseLocalDate(row.startDate) ?: return@mapNotNull null
        val (freqDays, doseLabel) = deriveDose(row.calculatorType, row.config) ?: return@mapNotNull null
        DerivedProtocol(
            id = row.id,
            label = row.label ?: row.calculatorType.replaceFirstChar { it.uppercase() },
            calculatorType = row.calculatorType,
            startDate = start,
            freqDays = freqDays,
            doseLabel = doseLabel,
        )
    }

/**
 * Is `date` a dose day for this protocol? A date is a dose day when (days since start)
 * lands on a multiple of freqDays. Fractional frequencies are handled by checking whether
 * the nearest dose index reproduces the exact day offset (identical to the web's
 * isDoseDay). Days before the start never qualify.
 */
fun isDoseDay(p: DerivedProtocol, date: LocalDate): Boolean {
    val d = java.time.temporal.ChronoUnit.DAYS.between(p.startDate, date)
    if (d < 0) return false
    val f = p.freqDays
    if (!f.isNum() || f <= 0) return false
    if (f == Math.floor(f)) return d % f.toLong() == 0L
    val k = (d / f).roundToLong()
    return (k * f).roundToLong() == d
}

/**
 * Project every dose across a [windowDays]-wide window starting at [start] (inclusive).
 * Defaults to the rolling 30-day window the calendar uses. Result is flat and date-sorted
 * within each day's protocol order — group by `date` for a day agenda or a month grid.
 */
fun projectDoses(
    protocols: List<DerivedProtocol>,
    start: LocalDate,
    windowDays: Int = 30,
): List<ProjectedDose> {
    val out = ArrayList<ProjectedDose>()
    for (offset in 0 until windowDays) {
        val date = start.plusDays(offset.toLong())
        for (p in protocols) {
            if (isDoseDay(p, date)) {
                out += ProjectedDose(
                    dosageId = p.id,
                    date = date,
                    label = p.label,
                    calculatorType = p.calculatorType,
                    doseLabel = p.doseLabel,
                )
            }
        }
    }
    return out
}

/** Convenience over [projectDoses] returning a per-day map for grid/agenda rendering. */
fun projectDosesByDay(
    rows: List<SavedDosage>,
    start: LocalDate,
    windowDays: Int = 30,
): Map<LocalDate, List<ProjectedDose>> =
    projectDoses(deriveProtocols(rows), start, windowDays).groupBy { it.date }
