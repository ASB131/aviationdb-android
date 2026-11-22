package uk.aviationdb.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uk.aviationdb.android.data.credentials.ApiKeyStore
import uk.aviationdb.android.ui.screens.AccountScreen
import uk.aviationdb.android.ui.screens.ExploreScreen
import uk.aviationdb.android.ui.screens.LiveScreen
import uk.aviationdb.android.ui.screens.SightingsScreen

private enum class Tab(val route: String, val label: String) {
    Live("live", "Live"),
    Sightings("sightings", "Sightings"),
    Explore("explore", "Explore"),
    Account("account", "Account"),
}

@Composable
fun AviationDbApp(apiKeyStore: ApiKeyStore) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    val selected = destination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    Tab.Live -> Icons.Default.Flight
                                    Tab.Sightings -> Icons.AutoMirrored.Filled.List
                                    Tab.Explore -> Icons.Default.Search
                                    Tab.Account -> Icons.Default.Person
                                },
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.Live.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Live.route) { LiveScreen(apiKeyStore) }
            composable(Tab.Sightings.route) { SightingsScreen(apiKeyStore) }
            composable(Tab.Explore.route) { ExploreScreen(apiKeyStore) }
            composable(Tab.Account.route) { AccountScreen(apiKeyStore) }
        }
    }
}