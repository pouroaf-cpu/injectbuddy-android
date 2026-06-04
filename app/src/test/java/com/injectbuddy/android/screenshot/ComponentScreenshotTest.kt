package com.injectbuddy.android.screenshot

import app.cash.paparazzi.Paparazzi
import com.injectbuddy.android.ui.components.EmptyState
import com.injectbuddy.android.ui.components.ErrorState
import org.junit.Rule
import org.junit.Test

/** Visual snapshots of the shared stateless state-views in the real theme, light + dark. */
class ComponentScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE)

    @Test
    fun emptyState_light() = paparazzi.themed(dark = false) {
        EmptyState(
            title = "No protocols yet",
            message = "Add your first protocol to see your schedule.",
            actionLabel = "Add a protocol",
            onAction = {},
        )
    }

    @Test
    fun emptyState_dark() = paparazzi.themed(dark = true) {
        EmptyState(
            title = "No protocols yet",
            message = "Add your first protocol to see your schedule.",
            actionLabel = "Add a protocol",
            onAction = {},
        )
    }

    @Test
    fun errorState_dark() = paparazzi.themed(dark = true) {
        ErrorState(message = "Couldn't load your dosages.", onRetry = {})
    }
}
