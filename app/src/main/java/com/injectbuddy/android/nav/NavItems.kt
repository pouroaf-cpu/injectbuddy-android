package com.injectbuddy.android.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * SINGLE SOURCE OF TRUTH for everything the navigation drawer lists. The web app keeps
 * its calculator list in one place (home.js CALC_ITEMS); this is the native twin. Never
 * duplicate the calculator list elsewhere — drawer, NavHost, and CalculatorScreen all
 * read from here.
 */
object NavItems {

    data class PrimaryItem(val route: String, val label: String, val icon: ImageVector)

    /** A calculator entry: [slug] drives the route + the CalculatorEngine spec lookup. */
    data class CalcItem(val slug: String, val label: String)

    val primary: List<PrimaryItem> = listOf(
        PrimaryItem(Routes.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
        PrimaryItem(Routes.CALENDAR, "Calendar", Icons.Filled.CalendarMonth),
    )

    /** The 14 calculators, slugs + labels matching the web app (home.js CALC_ITEMS). */
    val calculators: List<CalcItem> = listOf(
        CalcItem("trt-dose", "TRT Dose"),
        CalcItem("trt-eod", "TRT & EOD"),
        CalcItem("trt-microdose", "TRT Microdose"),
        CalcItem("hcg", "HCG"),
        CalcItem("peptide", "Peptide"),
        CalcItem("reconstitution", "Reconstitution"),
        CalcItem("semaglutide", "Semaglutide"),
        CalcItem("tirzepatide", "Tirzepatide"),
        CalcItem("retatrutide", "Retatrutide"),
        CalcItem("bpc-157", "BPC-157"),
        CalcItem("bpc-157-tb500", "BPC+TB500"),
        CalcItem("bmi", "BMI"),
        CalcItem("free-t-index", "Free T Index"),
        CalcItem("cycle-plotter", "Cycle Plotter"),
    )

    val settings = PrimaryItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
}
