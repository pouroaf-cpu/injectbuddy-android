package com.injectbuddy.android.screenshot

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.injectbuddy.android.ui.theme.InjectBuddyTheme

/**
 * Shared Paparazzi helpers so every screenshot test renders the app's REAL theme
 * (InjectBuddyTheme) headlessly — light + dark, phone + tablet — without an emulator,
 * ServiceLocator, or network. Snapshot PNGs are uploaded by CI and reviewed visually.
 */

/** Standard phone canvas. */
val PHONE: DeviceConfig = DeviceConfig.PIXEL_6

/** Tablet canvas — wide enough (>840dp) to exercise the permanent-drawer breakpoint. */
val TABLET: DeviceConfig = DeviceConfig.PIXEL_C

/** Render [content] inside the real app theme (explicit light/dark) over a Surface. */
fun Paparazzi.themed(dark: Boolean, content: @Composable () -> Unit) {
    snapshot {
        InjectBuddyTheme(darkTheme = dark) {
            Surface { content() }
        }
    }
}
