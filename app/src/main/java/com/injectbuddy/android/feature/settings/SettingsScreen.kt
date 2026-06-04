@file:OptIn(ExperimentalMaterial3Api::class)

package com.injectbuddy.android.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.injectbuddy.android.core.UiState
import com.injectbuddy.android.data.model.UserProfile
import com.injectbuddy.android.di.ServiceLocator
import com.injectbuddy.android.ui.theme.ThemeMode

/**
 * Settings (SCREENS.md §4): profile card, PREFERENCES (theme/units/syringe dropdowns),
 * CONNECTIONS (Discord link), ACCOUNT (change password / sign out / delete). Sign out +
 * delete are error-colored; delete confirms via AlertDialog. Theme is wired to the shared
 * ThemeController so the change is app-wide. Signature fixed — AppNavHost calls
 * SettingsScreen(openDrawer).
 */
@Composable
fun SettingsScreen(openDrawer: () -> Unit) {
    val vm: SettingsViewModel = viewModel {
        SettingsViewModel(
            ServiceLocator.accountRepository,
            ServiceLocator.authRepository,
            ServiceLocator.themeController,
        )
    }
    val profile by vm.profile.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val theme by vm.themeMode.collectAsStateWithLifecycle()

    SettingsContent(
        profile = profile,
        theme = theme,
        units = prefs.units,
        syringe = prefs.syringe,
        onTheme = vm::setTheme,
        onUnits = vm::setUnits,
        onSyringe = vm::setSyringe,
        onLinkDiscord = vm::linkDiscord,
        onSignOut = vm::signOut,
        onDeleteAccount = vm::deleteAccount,
        openDrawer = openDrawer,
    )
}

/**
 * Stateless settings UI — full Scaffold + TopAppBar + list + delete dialog, fed the loaded
 * profile/preferences and lambdas. Pure (no VM / ServiceLocator) so it renders headlessly
 * under Paparazzi. The delete-confirmation dialog's own visibility is local UI state.
 */
@Composable
internal fun SettingsContent(
    profile: UiState<UserProfile>,
    theme: ThemeMode,
    units: UnitSystem,
    syringe: SyringeScale,
    onTheme: (ThemeMode) -> Unit,
    onUnits: (UnitSystem) -> Unit,
    onSyringe: (SyringeScale) -> Unit,
    onLinkDiscord: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    openDrawer: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                    }
                },
            )
        },
    ) { padding ->
        SettingsList(
            padding = padding,
            profile = profile,
            theme = theme,
            units = units,
            syringe = syringe,
            onTheme = onTheme,
            onUnits = onUnits,
            onSyringe = onSyringe,
            onLinkDiscord = onLinkDiscord,
            onSignOut = onSignOut,
            onDelete = { showDeleteDialog = true },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete account?") },
            text = { Text("This permanently removes your account and saved protocols. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteAccount()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingsList(
    padding: PaddingValues,
    profile: UiState<UserProfile>,
    theme: ThemeMode,
    units: UnitSystem,
    syringe: SyringeScale,
    onTheme: (ThemeMode) -> Unit,
    onUnits: (UnitSystem) -> Unit,
    onSyringe: (SyringeScale) -> Unit,
    onLinkDiscord: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item { ProfileCard(profile) }

        item { SectionHeader("PREFERENCES") }
        item {
            DropdownRow(
                title = "Theme",
                selectedLabel = theme.label(),
                options = ThemeMode.entries.map { it to it.label() },
                onSelect = onTheme,
            )
        }
        item {
            DropdownRow(
                title = "Units",
                selectedLabel = units.label(),
                options = UnitSystem.entries.map { it to it.label() },
                onSelect = onUnits,
            )
        }
        item {
            DropdownRow(
                title = "Syringe scale",
                selectedLabel = syringe.label(),
                options = SyringeScale.entries.map { it to it.label() },
                onSelect = onSyringe,
            )
        }

        item { SectionHeader("CONNECTIONS") }
        item {
            NavRow(title = "Discord", trailing = "Link", onClick = onLinkDiscord)
        }

        item { SectionHeader("ACCOUNT") }
        item {
            // TODO(change-password): route to a reset-password flow / form.
            NavRow(title = "Change password", onClick = {})
        }
        item {
            ListItem(
                headlineContent = { Text("Sign out", color = MaterialTheme.colorScheme.error) },
                trailingContent = {
                    Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.clickable(onClick = onSignOut),
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Delete account", color = MaterialTheme.colorScheme.error) },
                trailingContent = {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.clickable(onClick = onDelete),
            )
        }
    }
}

@Composable
private fun ProfileCard(profile: UiState<UserProfile>) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        when (profile) {
            is UiState.Content -> {
                val p = profile.data
                val name = p.displayName ?: p.email?.substringBefore('@') ?: "User"
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                name.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        p.email?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            is UiState.Error -> Text(
                profile.message,
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error,
            )
            else -> Text("Loading…", Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** A ListItem with a trailing dropdown — used for theme / units / syringe scale. */
@Composable
private fun <T> DropdownRow(
    title: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ListItem(
            headlineContent = { Text(title) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun NavRow(title: String, trailing: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailing != null) {
                    Text(trailing, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

// ── label helpers ────────────────────────────────────────────────────────────────
private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun UnitSystem.label() = when (this) {
    UnitSystem.METRIC -> "Metric"
    UnitSystem.IMPERIAL -> "Imperial"
}

private fun SyringeScale.label() = when (this) {
    SyringeScale.U100 -> "U-100"
    SyringeScale.U40 -> "U-40"
}

// ── sample state (for Paparazzi snapshots) ─────────────────────────────────────────

/** A loaded profile for snapshot rendering of the settings screen. */
internal fun sampleSettingsProfile(): UiState<UserProfile> =
    UiState.Content(
        UserProfile(id = "u1", displayName = "Pouroa Frew", email = "you@example.com"),
    )

/** The default preference selections (Metric units, U-100 syringe) for snapshots. */
internal fun sampleSettingsPrefs(): SettingsPrefs = SettingsPrefs()
