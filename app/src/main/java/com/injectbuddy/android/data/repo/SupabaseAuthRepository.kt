package com.injectbuddy.android.data.repo

import com.injectbuddy.android.domain.AuthRepository
import com.injectbuddy.android.domain.AuthState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Discord
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Supabase-backed [AuthRepository]. The single source of truth for auth is supabase-kt's
 * own [io.github.jan.supabase.auth.Auth.sessionStatus]; we just MAP it to the app's
 * [AuthState] so the rest of the app never imports supabase types. Restoring a persisted
 * session (LoadingFromStorage) shows the splash spinner — hence Loading is the seed value.
 */
class SupabaseAuthRepository(
    private val supabase: SupabaseClient,
    private val scope: CoroutineScope,
) : AuthRepository {

    override val authState: StateFlow<AuthState> =
        supabase.auth.sessionStatus
            .map { status -> status.toAuthState() }
            .stateIn(
                scope = scope,
                // Eagerly so AppRoot always has a value and the session restore starts
                // immediately on cold start rather than on first collection.
                started = SharingStarted.Eagerly,
                initialValue = AuthState.Loading,
            )

    private fun SessionStatus.toAuthState(): AuthState = when (this) {
        is SessionStatus.Authenticated -> AuthState.Authenticated(
            userId = session.user?.id.orEmpty(),
            email = session.user?.email,
        )
        is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated
        // The initial storage read and a failed token refresh are both "not ready yet"
        // from the UI's perspective — keep showing the splash, not a false logout.
        // (3.x renamed LoadingFromStorage→Initializing and NetworkError→RefreshFailure.)
        is SessionStatus.Initializing -> AuthState.Loading
        is SessionStatus.RefreshFailure -> AuthState.Loading
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        supabase.auth.resetPasswordForEmail(email)
    }

    override suspend fun signInWithDiscord(): Result<Unit> = runCatching {
        // Opens the system browser / custom tab; the OAuth callback deep-links back via
        // the scheme configured in SupabaseProvider and flips sessionStatus.
        supabase.auth.signInWith(Discord)
    }

    override suspend fun signOut() {
        // Best-effort: sessionStatus flips to NotAuthenticated regardless; swallow any
        // network error so the user is always let out locally.
        runCatching { supabase.auth.signOut() }
    }
}
