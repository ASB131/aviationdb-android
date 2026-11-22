package uk.aviationdb.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.screens.AccountScreen
import uk.aviationdb.android.ui.screens.BrowseScreen
import uk.aviationdb.android.ui.screens.LiveScreen
import uk.aviationdb.android.ui.screens.SightingsScreen
import uk.aviationdb.android.ui.screens.StatsScreen
import uk.aviationdb.android.ui.theme.Navy
import uk.aviationdb.android.ui.theme.SkyBlue

private enum class Tab(val route: String, val label: String) {
    Live("live", "Live"),
    Sightings("sightings", "Sightings"),
    Stats("stats", "Stats"),
    Browse("browse", "Browse"),
    Account("account", "Account"),
}

@Composable
fun AviationDbApp(
    sessionStore: SessionStore,
    onSignedOut: () -> Unit,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
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
                                    Tab.Stats -> Icons.Default.BarChart
                                    Tab.Browse -> Icons.Default.Search
                                    Tab.Account -> Icons.Default.Person
                                },
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SkyBlue,
                            selectedTextColor = Navy,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
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
            composable(Tab.Live.route) { LiveScreen(sessionStore) }
            composable(Tab.Sightings.route) { SightingsScreen(sessionStore) }
            composable(Tab.Stats.route) { StatsScreen(sessionStore) }
            composable(Tab.Browse.route) { BrowseScreen(sessionStore) }
            composable(Tab.Account.route) { AccountScreen(sessionStore, onSignedOut) }
        }
    }
}