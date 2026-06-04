package com.injectbuddy.android.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * App-wide theme override. Held in-memory and observed by [InjectBuddyTheme]; the
 * Settings screen flips it. Persisting the choice to DataStore is a follow-up (see
 * TASK 8) — until then it resets to SYSTEM on cold start.
 */
class ThemeController {
    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        _mode.value = mode
    }
}
