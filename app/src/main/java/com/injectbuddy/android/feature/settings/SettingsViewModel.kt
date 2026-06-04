package com.injectbuddy.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.injectbuddy.android.core.UiState
import com.injectbuddy.android.data.model.UserProfile
import com.injectbuddy.android.domain.AuthRepository
import com.injectbuddy.android.domain.AccountRepository
import com.injectbuddy.android.ui.theme.ThemeController
import com.injectbuddy.android.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Unit + syringe-scale preferences. In-memory only for now (see DataStore TODO). */
enum class UnitSystem { METRIC, IMPERIAL }
enum class SyringeScale { U100, U40 }

/** Non-profile settings state — theme reads through to the ThemeController flow. */
data class SettingsPrefs(
    val units: UnitSystem = UnitSystem.METRIC,
    val syringe: SyringeScale = SyringeScale.U100,
)

/**
 * Settings backing VM. Owns the profile load (UiState) and the in-memory preference
 * state; theme changes are delegated to the shared [ThemeController] so the whole app
 * recomposes. Sign-out / Discord-link delegate to [AuthRepository]; AppRoot reacts to
 * authState (no manual navigation here).
 *
 * Units + syringe scale are in-memory only — persisting them (and theme) to DataStore is
 * the follow-up tracked on ThemeController. Marked with TODOs so it isn't lost.
 */
class SettingsViewModel(
    private val account: AccountRepository,
    private val auth: AuthRepository,
    private val themeController: ThemeController,
) : ViewModel() {

    private val _profile = MutableStateFlow<UiState<UserProfile>>(UiState.Loading)
    val profile: StateFlow<UiState<UserProfile>> = _profile.asStateFlow()

    private val _prefs = MutableStateFlow(SettingsPrefs())
    val prefs: StateFlow<SettingsPrefs> = _prefs.asStateFlow()

    /** The live theme mode, surfaced for the dropdown selection. */
    val themeMode: StateFlow<ThemeMode> = themeController.mode

    init {
        loadProfile()
    }

    fun loadProfile() {
        _profile.value = UiState.Loading
        viewModelScope.launch {
            account.getProfile()
                .onSuccess { _profile.value = UiState.Content(it) }
                .onFailure { _profile.value = UiState.Error(it.message ?: "Couldn't load your profile") }
        }
    }

    fun setTheme(mode: ThemeMode) = themeController.set(mode)

    // TODO(datastore): persist units + syringe scale (and theme) across cold starts.
    fun setUnits(units: UnitSystem) = _prefs.update { it.copy(units = units) }
    fun setSyringe(scale: SyringeScale) = _prefs.update { it.copy(syringe = scale) }

    /** Discord link mirrors the web's /discord-link — same OAuth entry point as login. */
    fun linkDiscord() {
        viewModelScope.launch { auth.signInWithDiscord() }
    }

    fun signOut() {
        // AppRoot observes authState and swaps back to AuthNavGraph once cleared.
        viewModelScope.launch { auth.signOut() }
    }

    /**
     * TODO(account-delete): no server-side account deletion endpoint exists yet. Until
     * one lands, "delete" can only sign the user out locally — wired here so the UI flow
     * (confirm dialog → action) is complete and the backend call is a single drop-in.
     */
    fun deleteAccount() {
        viewModelScope.launch { auth.signOut() }
    }
}
