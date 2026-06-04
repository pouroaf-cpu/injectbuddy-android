package com.injectbuddy.android.screenshot

import app.cash.paparazzi.Paparazzi
import com.injectbuddy.android.feature.auth.LoginContent
import com.injectbuddy.android.feature.auth.ResetContent
import com.injectbuddy.android.feature.auth.SignUpContent
import com.injectbuddy.android.feature.auth.sampleLoginState
import com.injectbuddy.android.feature.auth.sampleResetState
import com.injectbuddy.android.feature.auth.sampleSignUpState
import org.junit.Rule
import org.junit.Test

/**
 * The pre-auth forms (login / sign-up / reset), rendered from the stateless *Content
 * composables fed sample form state + no-op callbacks, in the real theme.
 */
class AuthScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE)

    @Test
    fun login_light() = paparazzi.themed(dark = false) {
        LoginContent(
            state = sampleLoginState(),
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onDiscord = {},
            onForgot = {},
            onSignUp = {},
        )
    }

    @Test
    fun login_dark() = paparazzi.themed(dark = true) {
        LoginContent(
            state = sampleLoginState(),
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onDiscord = {},
            onForgot = {},
            onSignUp = {},
        )
    }

    @Test
    fun signUp_light() = paparazzi.themed(dark = false) {
        SignUpContent(
            state = sampleSignUpState(),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmChange = {},
            onSignUp = {},
            onDiscord = {},
            onBackToLogin = {},
        )
    }

    @Test
    fun reset_light() = paparazzi.themed(dark = false) {
        ResetContent(
            state = sampleResetState(),
            onEmailChange = {},
            onSendReset = {},
            onBackToLogin = {},
        )
    }
}
