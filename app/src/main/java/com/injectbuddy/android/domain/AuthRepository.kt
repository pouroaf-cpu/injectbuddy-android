package com.injectbuddy.android.domain

import kotlinx.coroutines.flow.StateFlow

/** Current authentication state, observed by [com.injectbuddy.android.AppRoot]. */
sealed interface AuthState {
    /** Session is being restored on cold start — show a splash/spinner. */
    data object Loading : AuthState
    data class Authenticated(val userId: String, val email: String?) : AuthState
    data object Unauthenticated : AuthState
}

/**
 * Auth boundary over Supabase. The concrete implementation (SupabaseAuthRepository)
 * lives in data/repo and is wired in [com.injectbuddy.android.di.ServiceLocator].
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun signInWithDiscord(): Result<Unit>
    suspend fun signOut()
}
