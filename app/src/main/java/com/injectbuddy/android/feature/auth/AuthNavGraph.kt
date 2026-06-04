package com.injectbuddy.android.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.injectbuddy.android.di.ServiceLocator
import com.injectbuddy.android.domain.AuthRepository

/**
 * Pre-auth flow: a self-contained NavController over login/signup/reset. No drawer, no
 * MainShell. On a successful sign-in/up, AppRoot recomposes to MainShell off authState —
 * these screens never navigate "into" the app themselves; they only route between each
 * other. Signature is fixed: AppRoot calls AuthNavGraph(auth).
 */

private object AuthRoutes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val RESET = "reset"
}

@Composable
fun AuthNavGraph(auth: AuthRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AuthRoutes.LOGIN) {
        composable(AuthRoutes.LOGIN) {
            LoginScreen(
                onSignUp = { navController.navigate(AuthRoutes.SIGNUP) },
                onForgot = { navController.navigate(AuthRoutes.RESET) },
            )
        }
        composable(AuthRoutes.SIGNUP) {
            SignUpScreen(onBackToLogin = { navController.popBackStack() })
        }
        composable(AuthRoutes.RESET) {
            ResetScreen(onBackToLogin = { navController.popBackStack() })
        }
    }
}

/** ViewModel factory reading the shared [AuthRepository] from the ServiceLocator. */
@Composable
private fun authViewModel(): AuthViewModel = viewModel {
    AuthViewModel(ServiceLocator.authRepository)
}

@Composable
private fun LoginScreen(onSignUp: () -> Unit, onForgot: () -> Unit) {
    val vm = authViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    AuthScaffold(title = "injectbuddy", subtitle = null) {
        AuthTextField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            label = "Email",
            error = state.emailError,
            keyboardType = KeyboardType.Email,
        )
        PasswordField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = "Password",
            error = state.passwordError,
            imeAction = ImeAction.Done,
            onImeAction = { if (state.loginValid) vm.signIn() },
        )
        FormError(state.error)
        PrimaryButton(
            text = "Sign in",
            enabled = state.loginValid && !state.submitting,
            loading = state.submitting,
            onClick = vm::signIn,
        )
        OrDivider()
        DiscordButton(enabled = !state.submitting, onClick = vm::signInWithDiscord)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onForgot) { Text("Forgot?") }
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onSignUp) { Text("Sign up") }
        }
    }
}

@Composable
private fun SignUpScreen(onBackToLogin: () -> Unit) {
    val vm = authViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    AuthScaffold(title = "injectbuddy", subtitle = "Create account", onBack = onBackToLogin) {
        AuthTextField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            label = "Email",
            error = state.emailError,
            keyboardType = KeyboardType.Email,
        )
        PasswordField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = "Password",
            error = state.passwordError,
        )
        PasswordField(
            value = state.confirm,
            onValueChange = vm::onConfirmChange,
            label = "Confirm password",
            error = state.confirmError(),
            imeAction = ImeAction.Done,
            onImeAction = { if (state.signUpValid) vm.signUp() },
        )
        FormError(state.error)
        PrimaryButton(
            text = "Sign up",
            enabled = state.signUpValid && !state.submitting,
            loading = state.submitting,
            onClick = vm::signUp,
        )
        OrDivider()
        DiscordButton(enabled = !state.submitting, onClick = vm::signInWithDiscord)
        TextButton(onClick = onBackToLogin) { Text("Have an account? Sign in") }
    }
}

@Composable
private fun ResetScreen(onBackToLogin: () -> Unit) {
    val vm = authViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    AuthScaffold(title = "Reset password", subtitle = null, onBack = onBackToLogin) {
        if (state.resetSent) {
            Text(
                "Check your email for a reset link.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        } else {
            AuthTextField(
                value = state.email,
                onValueChange = vm::onEmailChange,
                label = "Email",
                error = state.emailError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                onImeAction = { if (state.resetValid) vm.sendReset() },
            )
            FormError(state.error)
            PrimaryButton(
                text = "Send reset",
                enabled = state.resetValid && !state.submitting,
                loading = state.submitting,
                onClick = vm::sendReset,
            )
        }
        TextButton(onClick = onBackToLogin) { Text("← Back to login") }
    }
}

// ── shared building blocks ───────────────────────────────────────────────────────

@Composable
private fun AuthScaffold(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onBack != null) {
                TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                    Text("← Back")
                }
            }
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

/** Validated text field with inline supportingText (SCREENS.md §3). */
@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(text)
        }
    }
}

@Composable
private fun DiscordButton(enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Continue with Discord")
    }
}

@Composable
private fun OrDivider() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text("  or  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FormError(message: String?) {
    if (message != null) {
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
