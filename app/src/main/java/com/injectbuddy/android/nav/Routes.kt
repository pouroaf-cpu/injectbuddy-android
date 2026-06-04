package com.injectbuddy.android.nav

/** Centralised route strings for the authenticated NavHost. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"

    const val CALCULATOR_ARG_SLUG = "slug"
    const val CALCULATOR_PATTERN = "calculator/{$CALCULATOR_ARG_SLUG}"

    fun calculator(slug: String) = "calculator/$slug"
}
