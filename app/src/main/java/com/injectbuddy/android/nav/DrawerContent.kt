package com.injectbuddy.android.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.injectbuddy.android.data.model.UserProfile

/**
 * Drawer sheet contents — profile header, primary items, the 14 calculators, and
 * Settings. Sheet-agnostic: the caller wraps it in a ModalDrawerSheet (phone) or a
 * PermanentDrawerSheet (tablet) so we don't nest two sheets.
 */
@Composable
fun DrawerContent(
    currentRoute: String?,
    profile: UserProfile?,
    onNavigate: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp),
    ) {
            ProfileHeader(profile)
            HorizontalDivider()

            Spacer(Modifier.height(8.dp))
            NavItems.primary.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item.label) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "CALCULATORS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 4.dp),
            )
            NavItems.calculators.forEach { calc ->
                val route = Routes.calculator(calc.slug)
                NavigationDrawerItem(
                    label = { Text(calc.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.Filled.Calculate, contentDescription = null) },
                    selected = currentRoute == route,
                    onClick = { onNavigate(route) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            NavigationDrawerItem(
                label = { Text(NavItems.settings.label) },
                icon = { Icon(NavItems.settings.icon, contentDescription = null) },
                selected = currentRoute == NavItems.settings.route,
                onClick = { onNavigate(NavItems.settings.route) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }

@Composable
private fun ProfileHeader(profile: UserProfile?) {
    val name = profile?.displayName ?: profile?.email?.substringBefore('@') ?: "Signed in"
    val initials = name.take(2).uppercase()
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        profile?.email?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
