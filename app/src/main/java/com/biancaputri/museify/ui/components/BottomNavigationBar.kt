package com.biancaputri.museify.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.biancaputri.museify.R

enum class MuseifyTopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    HOME(
        route = "home",
        labelRes = R.string.nav_home,
        icon = Icons.Rounded.Home
    ),
    SEARCH(
        route = "search",
        labelRes = R.string.nav_search,
        icon = Icons.Rounded.Search
    ),
    FAVORITES(
        route = "favorites",
        labelRes = R.string.nav_favorites,
        icon = Icons.Rounded.Favorite
    )
}

@Composable
fun MuseifyBottomBar(
    currentRoute: String?,
    onNavigate: (MuseifyTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        MuseifyTopLevelDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = label
                    )
                },
                label = { Text(text = label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun MuseifyNavigationRail(
    currentRoute: String?,
    onNavigate: (MuseifyTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Spacer(modifier = Modifier.weight(0.15f))
        MuseifyTopLevelDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            val label = stringResource(destination.labelRes)
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                modifier = Modifier.padding(vertical = 4.dp),
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = label
                    )
                },
                label = { Text(text = label, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.weight(0.85f))
    }
}
