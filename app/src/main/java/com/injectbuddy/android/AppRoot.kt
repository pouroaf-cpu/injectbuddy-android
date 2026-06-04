package com.injectbuddy.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.injectbuddy.android.di.ServiceLocator
import com.injectbuddy.android.domain.AuthState
import com.injectbuddy.android.feature.auth.AuthNavGraph
import com.injectbuddy.android.nav.MainShell

/**
 * Top of the Compose tree. Switches the whole app on auth state: unauthenticated users
 * see the auth flow; authenticated users land in [MainShell] (dashboard). No guest mode.
 */
@Composable
fun AppRoot() {
    val auth = ServiceLocator.authRepository
    val state by auth.authState.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        when (state) {
            is AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is AuthState.Unauthenticated -> AuthNavGraph(auth)
            is AuthState.Authenticated -> MainShell()
        }
    }
}
