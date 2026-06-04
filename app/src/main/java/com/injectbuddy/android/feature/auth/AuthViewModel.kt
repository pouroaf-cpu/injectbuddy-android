package com.injectbuddy.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.injectbuddy.android.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives all three pre-auth forms (login / sign-up / reset) from one VM — they share the
 * same email/password fields, validation, and submit lifecycle. On success the VM does
 * NOT navigate: AppRoot swaps to MainShell off authState. For reset, success is a flag
 * the screen turns into a confirmation message.
 */
class AuthViewModel(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthFormState())
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, error = null) }

    fun onConfirmChange(value: String) =
        _state.update { it.copy(confirm = value, error = null) }

    /** Clear transient state when moving between login/signup/reset routes. */
    fun reset() = _state.update {
        AuthFormState(email = it.email) // keep the email so re-typing isn't needed
    }

    fun signIn() = submit(requirePassword = true) {
        auth.signIn(_state.value.email.trim(), _state.value.password)
    }

    fun signUp() = submit(requirePassword = true, requireConfirm = true) {
        auth.signUp(_state.value.email.trim(), _state.value.password)
    }

    fun sendReset() = submit(requirePassword = false) {
        auth.sendPasswordReset(_state.value.email.trim()).onSuccess {
            _state.update { it.copy(resetSent = true) }
        }
    }

    fun signInWithDiscord() = submit(requirePassword = false) {
        auth.signInWithDiscord()
    }

    /**
     * Shared submit wrapper: validate, flip to submitting, run, surface a friendly error.
     * The block returns the repository [Result] so a failure becomes an inline message.
     */
    private fun submit(
        requirePassword: Boolean,
        requireConfirm: Boolean = false,
        block: suspend () -> Result<Unit>,
    ) {
        val s = _state.value
        validate(s, requirePassword, requireConfirm)?.let { msg ->
            _state.update { it.copy(error = msg) }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = block()
            _state.update {
                it.copy(
                    submitting = false,
                    error = result.exceptionOrNull()?.let(::friendlyError),
                )
            }
        }
    }

    private fun validate(
        s: AuthFormState,
        requirePassword: Boolean,
        requireConfirm: Boolean,
    ): String? = when {
        !isEmailValid(s.email) -> "Enter a valid email"
        requirePassword && s.password.length < 6 -> "Password must be at least 6 characters"
        requireConfirm && s.password != s.confirm -> "Passwords don't match"
        else -> null
    }

    private fun friendlyError(t: Throwable): String =
        t.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Try again."

    companion object {
        fun isEmailValid(email: String): Boolean {
            val e = email.trim()
            // Lightweight check (Android's Patterns isn't available in unit tests / pure VM).
            return e.length in 3..254 && "@" in e && e.substringAfter('@').contains('.')
        }
    }
}

/** UI state shared by the three auth forms. */
data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
    val resetSent: Boolean = false,
) {
    val emailError: String?
        get() = if (email.isNotEmpty() && !AuthViewModel.isEmailValid(email)) "Enter a valid email" else null

    val passwordError: String?
        get() = if (password.isNotEmpty() && password.length < 6) "At least 6 characters" else null

    fun confirmError(): String? =
        if (confirm.isNotEmpty() && confirm != password) "Passwords don't match" else null

    val loginValid: Boolean
        get() = AuthViewModel.isEmailValid(email) && password.length >= 6

    val signUpValid: Boolean
        get() = loginValid && confirm == password

    val resetValid: Boolean
        get() = AuthViewModel.isEmailValid(email)
}
