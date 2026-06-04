package com.injectbuddy.android.screenshot

import app.cash.paparazzi.Paparazzi
import com.injectbuddy.android.core.UiState
import com.injectbuddy.android.feature.calendar.CalendarContent
import com.injectbuddy.android.feature.calendar.sampleCalendarData
import org.junit.Rule
import org.junit.Test

/**
 * The calendar, rendered from the stateless CalendarContent fed a populated month (dose
 * dots + a selected-day agenda) and no-op callbacks, in light + dark.
 */
class CalendarScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE)

    @Test
    fun calendar_light() = paparazzi.themed(dark = false) {
        CalendarContent(
            state = UiState.Content(sampleCalendarData()),
            openDrawer = {},
            onRetry = {},
            onPrevMonth = {},
            onNextMonth = {},
            onToday = {},
            onSelectDay = {},
        )
    }

    @Test
    fun calendar_dark() = paparazzi.themed(dark = true) {
        CalendarContent(
            state = UiState.Content(sampleCalendarData()),
            openDrawer = {},
            onRetry = {},
            onPrevMonth = {},
            onNextMonth = {},
            onToday = {},
            onSelectDay = {},
        )
    }
}
