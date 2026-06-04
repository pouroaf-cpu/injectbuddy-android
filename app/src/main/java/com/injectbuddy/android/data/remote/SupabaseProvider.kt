package com.injectbuddy.android.data.remote

import com.injectbuddy.android.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Builds the single [SupabaseClient] for the app — Auth (email + OAuth) and Postgrest
 * (RLS-scoped table access). URL + anon key come from BuildConfig, injected from the
 * gitignored local.properties (never hard-coded).
 */
object SupabaseProvider {
    fun create(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth) {
            // Deep link the OAuth (Discord) callback back into the app.
            scheme = "injectbuddy"
            host = "auth-callback"
        }
        install(Postgrest)
    }
}
