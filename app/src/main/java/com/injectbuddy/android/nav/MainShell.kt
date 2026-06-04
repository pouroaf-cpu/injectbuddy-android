package com.injectbuddy.android.nav

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

/**
 * Authenticated shell: the navigation drawer ("sidebar") over the content NavHost.
 * Phones get a swipe-/scrim-driven [ModalNavigationDrawer]; tablets and landscape get an
 * always-visible [PermanentNavigationDrawer] (the wireframe's tablet column).
 */
@Composable
fun MainShell() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigateTo(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(Routes.DASHBOARD) { saveState = true }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val permanent = maxWidth >= 840.dp

        if (permanent) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet {
                        DrawerContent(currentRoute = currentRoute, onNavigate = ::navigateTo)
                    }
                },
            ) {
                AppNavHost(navController = navController, openDrawer = {})
            }
        } else {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        DrawerContent(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                scope.launch { drawerState.close() }
                                navigateTo(route)
                            },
                        )
                    }
                },
            ) {
                AppNavHost(
                    navController = navController,
                    openDrawer = { scope.launch { drawerState.open() } },
                )
            }
        }
    }
}
