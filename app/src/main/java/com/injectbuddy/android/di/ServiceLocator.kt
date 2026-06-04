package com.injectbuddy.android.di

import android.content.Context
import com.injectbuddy.android.data.remote.SupabaseProvider
import com.injectbuddy.android.data.repo.SupabaseAccountRepository
import com.injectbuddy.android.data.repo.SupabaseAuthRepository
import com.injectbuddy.android.data.repo.SupabaseCycleRepository
import com.injectbuddy.android.domain.AccountRepository
import com.injectbuddy.android.domain.AuthRepository
import com.injectbuddy.android.domain.CycleRepository
import com.injectbuddy.android.ui.theme.ThemeController
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container. Lightweight alternative to Hilt for a greenfield app —
 * a single object holding app-scoped singletons, initialised from [InjectBuddyApp].
 */
object ServiceLocator {

    private lateinit var appContext: Context

    val appScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    val supabase: SupabaseClient by lazy { SupabaseProvider.create() }

    val themeController: ThemeController by lazy { ThemeController() }

    val authRepository: AuthRepository by lazy { SupabaseAuthRepository(supabase, appScope) }

    val accountRepository: AccountRepository by lazy { SupabaseAccountRepository(supabase) }

    val cycleRepository: CycleRepository by lazy { SupabaseCycleRepository(supabase) }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
