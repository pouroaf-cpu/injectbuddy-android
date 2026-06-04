package com.injectbuddy.android.screenshot

import app.cash.paparazzi.Paparazzi
import com.injectbuddy.android.feature.settings.SettingsContent
import com.injectbuddy.android.feature.settings.sampleSettingsPrefs
import com.injectbuddy.android.feature.settings.sampleSettingsProfile
import com.injectbuddy.android.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

/**
 * The settings screen, rendered from the stateless SettingsContent fed a loaded profile +
 * preference selections + no-op callbacks, in light + dark.
 */
class SettingsScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE)

    @Test
    fun settings_light() = paparazzi.themed(dark = false) {
        val prefs = sampleSettingsPrefs()
        SettingsContent(
            profile = sampleSettingsProfile(),
            theme = ThemeMode.LIGHT,
            units = prefs.units,
            syringe = prefs.syringe,
            onTheme = {},
            onUnits = {},
            onSyringe = {},
            onLinkDiscord = {},
            onSignOut = {},
            onDeleteAccount = {},
            openDrawer = {},
        )
    }

    @Test
    fun settings_dark() = paparazzi.themed(dark = true) {
        val prefs = sampleSettingsPrefs()
        SettingsContent(
            profile = sampleSettingsProfile(),
            theme = ThemeMode.DARK,
            units = prefs.units,
            syringe = prefs.syringe,
            onTheme = {},
            onUnits = {},
            onSyringe = {},
            onLinkDiscord = {},
            onSignOut = {},
            onDeleteAccount = {},
            openDrawer = {},
        )
    }
}
