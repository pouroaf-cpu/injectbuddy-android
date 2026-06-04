package com.injectbuddy.android.screenshot

import androidx.compose.material3.ModalDrawerSheet
import app.cash.paparazzi.Paparazzi
import com.injectbuddy.android.data.model.UserProfile
import com.injectbuddy.android.nav.DrawerContent
import com.injectbuddy.android.nav.Routes
import org.junit.Rule
import org.junit.Test

/**
 * The navigation drawer — the app's signature "sidebar" listing all 14 calculators.
 * Rendered with a sample profile (DrawerContent is now stateless) in light + dark.
 */
class DrawerScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE)

    private val sampleProfile = UserProfile(
        id = "sample",
        displayName = "Pouroa Frew",
        email = "you@example.com",
        avatarUrl = null,
    )

    @Test
    fun drawer_light() = paparazzi.themed(dark = false) {
        ModalDrawerSheet {
            DrawerContent(currentRoute = Routes.DASHBOARD, profile = sampleProfile, onNavigate = {})
        }
    }

    @Test
    fun drawer_dark() = paparazzi.themed(dark = true) {
        ModalDrawerSheet {
            DrawerContent(currentRoute = Routes.DASHBOARD, profile = sampleProfile, onNavigate = {})
        }
    }
}
