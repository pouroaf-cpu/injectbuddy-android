package com.injectbuddy.android.screenshot

import app.cash.paparazzi.Paparazzi
import com.injectbuddy.android.core.UiState
import com.injectbuddy.android.feature.dashboard.DashboardContent
import com.injectbuddy.android.feature.dashboard.sampleDashboardData
import com.injectbuddy.android.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

/**
 * The dashboard, rendered from the stateless DashboardContent fed a UiState + no-op
 * callbacks: the populated content state (light + dark) and the empty state (light).
 */
class DashboardScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE)

    @Test
    fun dashboard_populated_light() = paparazzi.themed(dark = false) {
        DashboardContent(
            state = UiState.Content(sampleDashboardData()),
            themeMode = ThemeMode.LIGHT,
            openDrawer = {},
            onOpenCalculator = {},
            onRetry = {},
            onToggleTheme = {},
        )
    }

    @Test
    fun dashboard_populated_dark() = paparazzi.themed(dark = true) {
        DashboardContent(
            state = UiState.Content(sampleDashboardData()),
            themeMode = ThemeMode.DARK,
            openDrawer = {},
            onOpenCalculator = {},
            onRetry = {},
            onToggleTheme = {},
        )
    }

    @Test
    fun dashboard_empty_light() = paparazzi.themed(dark = false) {
        DashboardContent(
            state = UiState.Empty,
            themeMode = ThemeMode.LIGHT,
            openDrawer = {},
            onOpenCalculator = {},
            onRetry = {},
            onToggleTheme = {},
        )
    }
}
