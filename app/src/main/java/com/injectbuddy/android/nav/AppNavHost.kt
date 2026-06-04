package com.injectbuddy.android.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.injectbuddy.android.feature.calculator.CalculatorScreen
import com.injectbuddy.android.feature.calendar.CalendarScreen
import com.injectbuddy.android.feature.dashboard.DashboardScreen
import com.injectbuddy.android.feature.settings.SettingsScreen

/**
 * Authenticated content graph. Dashboard is the start destination (app-first). Each
 * screen owns its own Scaffold/TopAppBar; [openDrawer] wires the menu button back to
 * the shared drawer in [MainShell].
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier,
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                openDrawer = openDrawer,
                onOpenCalculator = { slug -> navController.navigate(Routes.calculator(slug)) },
            )
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(openDrawer = openDrawer)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(openDrawer = openDrawer)
        }
        composable(
            route = Routes.CALCULATOR_PATTERN,
            arguments = listOf(navArgument(Routes.CALCULATOR_ARG_SLUG) { type = NavType.StringType }),
        ) { entry ->
            val slug = entry.arguments?.getString(Routes.CALCULATOR_ARG_SLUG).orEmpty()
            CalculatorScreen(
                slug = slug,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
