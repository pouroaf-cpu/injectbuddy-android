package com.injectbuddy.android.calendar

import com.injectbuddy.android.data.model.SavedDosage
import com.injectbuddy.android.feature.calendar.DerivedProtocol
import com.injectbuddy.android.feature.calendar.deriveProtocols
import com.injectbuddy.android.feature.calendar.isDoseDay
import com.injectbuddy.android.feature.calendar.projectDoses
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Locks down the dose-projection math (the Kotlin twin of the web's account-schedule
 * isDoseDay). Covers integer, fractional, EOD, and daily intervals plus the 30-day
 * window — the schedule is the load-bearing correctness surface of the calendar.
 */
class DoseProjectionTest {

    private val start = LocalDate.of(2026, 6, 1) // a Monday

    private fun dosage(
        calc: String,
        config: JsonObject,
        startDate: String? = "2026-06-01",
        active: Boolean = true,
    ) = SavedDosage(
        id = "row-$calc",
        calculatorType = calc,
        label = calc,
        config = config,
        startDate = startDate,
        isActive = active,
    )

    @Test
    fun `weekly TRT projects every 7th day across 30 days`() {
        // nDays = 7 → dose on day 0, 7, 14, 21, 28 within the window.
        val rows = listOf(
            dosage(
                "trt-dose",
                buildJsonObject {
                    put("mgWeek", JsonPrimitive(100))
                    put("strength", JsonPrimitive(250))
                    put("nDays", JsonPrimitive(7))
                },
            ),
        )
        val doses = projectDoses(deriveProtocols(rows), start, windowDays = 30)
        val days = doses.map { it.date }.toSet()

        assertEquals(
            setOf(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 8),
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 22),
                LocalDate.of(2026, 6, 29),
            ),
            days,
        )
    }

    @Test
    fun `EOD projects every other day`() {
        val rows = listOf(
            dosage(
                "trt-eod",
                buildJsonObject {
                    put("mgWeek", JsonPrimitive(100))
                    put("strength", JsonPrimitive(200))
                },
            ),
        )
        val days = projectDoses(deriveProtocols(rows), start, windowDays = 10).map { it.date }
        // Every 2 days from Jun 1: 1,3,5,7,9.
        assertEquals(
            listOf(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 7),
                LocalDate.of(2026, 6, 9),
            ),
            days,
        )
    }

    @Test
    fun `fractional E3-5D interval rounds each dose index to a real day`() {
        // peptide injPerWeek = 2 → freqDays = 3.5. The web's isDoseDay rounds each dose
        // index back to a whole day: a day at offset d is a dose day iff round(round(d/f)*f)
        // == d. For f = 3.5 that yields offsets 0, 4 (3.5→4), 7, 11 (10.5→11), 14 … — the
        // half-day doses land on a real calendar day rather than vanishing. Offset 3 is NOT
        // a dose day (its nearest index, 3.5, rounds to 4, not 3). This mirrors the web
        // exactly (lib/account-schedule.ts isDoseDay).
        val p = DerivedProtocol(
            id = "p",
            label = "Pep",
            calculatorType = "peptide",
            startDate = start,
            freqDays = 3.5,
            doseLabel = "250 mcg",
        )
        assertTrue(isDoseDay(p, LocalDate.of(2026, 6, 1)))  // offset 0
        assertFalse(isDoseDay(p, LocalDate.of(2026, 6, 4))) // offset 3 → nearest index rounds to 4
        assertTrue(isDoseDay(p, LocalDate.of(2026, 6, 5)))  // offset 4 (3.5 rounds here)
        assertTrue(isDoseDay(p, LocalDate.of(2026, 6, 8)))  // offset 7
        assertTrue(isDoseDay(p, LocalDate.of(2026, 6, 12))) // offset 11 (10.5 rounds here)
        assertTrue(isDoseDay(p, LocalDate.of(2026, 6, 15))) // offset 14
    }

    @Test
    fun `daily BPC-157 projects every day in the window`() {
        val rows = listOf(
            dosage("bpc-157", buildJsonObject { put("dose", JsonPrimitive(250)) }),
        )
        val doses = projectDoses(deriveProtocols(rows), start, windowDays = 30)
        assertEquals(30, doses.size)
        assertEquals("250.0 mcg", doses.first().doseLabel)
    }

    @Test
    fun `inactive and non-schedulable rows are dropped`() {
        val rows = listOf(
            dosage("trt-dose", buildJsonObject { put("nDays", JsonPrimitive(7)) }, active = false),
            dosage("bmi", buildJsonObject {}),
            dosage("reconstitution", buildJsonObject {}),
            dosage("trt-dose", buildJsonObject { put("nDays", JsonPrimitive(7)) }, startDate = null),
        )
        assertTrue(deriveProtocols(rows).isEmpty())
    }

    @Test
    fun `dates before start are never dose days`() {
        val p = DerivedProtocol(
            id = "p",
            label = "T",
            calculatorType = "trt-dose",
            startDate = start,
            freqDays = 7.0,
            doseLabel = "50 mg",
        )
        assertFalse(isDoseDay(p, start.minusDays(7)))
        assertFalse(isDoseDay(p, start.minusDays(1)))
        assertTrue(isDoseDay(p, start))
    }
}
