package com.injectbuddy.android.screenshot

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.injectbuddy.android.ui.components.EmptyState
import com.injectbuddy.android.ui.components.ErrorState
import com.injectbuddy.android.ui.theme.Teal
import org.junit.Rule
import org.junit.Test

/**
 * Headless visual snapshots (Paparazzi renders Compose to PNG on the JVM — no emulator).
 * This first test proves the render → artifact → review loop on the public stateless
 * components; full-screen snapshots follow once the pipeline is green.
 */
class ComponentScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    private fun snap(dark: Boolean, content: @Composable () -> Unit) {
        val scheme = if (dark) {
            darkColorScheme(primary = Teal)
        } else {
            lightColorScheme(primary = Teal)
        }
        paparazzi.snapshot {
            MaterialTheme(colorScheme = scheme) {
                Surface { content() }
            }
        }
    }

    @Test
    fun emptyState_light() = snap(dark = false) {
        EmptyState(
            title = "No protocols yet",
            message = "Add your first protocol to see your schedule.",
            actionLabel = "Add a protocol",
            onAction = {},
        )
    }

    @Test
    fun emptyState_dark() = snap(dark = true) {
        EmptyState(
            title = "No protocols yet",
            message = "Add your first protocol to see your schedule.",
            actionLabel = "Add a protocol",
            onAction = {},
        )
    }

    @Test
    fun errorState_light() = snap(dark = false) {
        ErrorState(message = "Couldn't load your dosages.", onRetry = {})
    }
}
