package com.injectbuddy.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.injectbuddy.android.di.ServiceLocator

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = LightSurface,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = TealDark,
    secondary = TealDark,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    error = ErrorRed,
)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = DarkBackground,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealContainerLight,
    secondary = Teal,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    error = ErrorRed,
)

@Composable
fun InjectBuddyTheme(content: @Composable () -> Unit) {
    val mode by ServiceLocator.themeController.mode.collectAsStateWithLifecycle()
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
