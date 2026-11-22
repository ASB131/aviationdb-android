package uk.aviationdb.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.navigation.LocalOpenMenu
import uk.aviationdb.android.ui.screens.AccountScreen
import uk.aviationdb.android.ui.screens.AircraftDetailScreen
import uk.aviationdb.android.ui.screens.AirlineDetailScreen
import uk.aviationdb.android.ui.screens.AirportDetailScreen
import uk.aviationdb.android.ui.screens.BrowseScreen
import uk.aviationdb.android.ui.screens.CallsignDetailScreen
import uk.aviationdb.android.ui.screens.LiveScreen
import uk.aviationdb.android.ui.screens.SightingsScreen
import uk.aviationdb.android.ui.screens.StatsScreen
import uk.aviationdb.android.ui.theme.Navy
import uk.aviationdb.android.ui.theme.SkyBlue

private enum class AppSection(val route: String, val label: String) {
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    CompositionLocalProvider(
        LocalOpenMenu provides {
            scope.launch { drawerState.open() }
        },
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Text(
                    "AviationDB",
                    style = MaterialTheme.typography.titleLarge,
                    color = Navy,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                )
                AppSection.entries.forEach { section ->
                    val selected = route == section.route
                    NavigationDrawerItem(
                        label = { Text(section.label) },
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            nav.navigate(section.route) {
                                popUpTo(AppSection.Live.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                when (section) {
                                    AppSection.Live -> Icons.Default.Flight
                                    AppSection.Sightings -> Icons.AutoMirrored.Filled.List
                                    AppSection.Stats -> Icons.Default.BarChart
                                    AppSection.Browse -> Icons.Default.Search
                                    AppSection.Account -> Icons.Default.Person
                                },
                                contentDescription = null,
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = Navy,
                            selectedIconColor = SkyBlue,
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            },
        ) {
            NavHost(
                navController = nav,
                startDestination = AppSection.Live.route,
            ) {
                composable(AppSection.Live.route) { LiveScreen(sessionStore, nav) }
                composable(AppSection.Sightings.route) { SightingsScreen(sessionStore) }
                composable(AppSection.Stats.route) { StatsScreen(sessionStore) }
                composable(AppSection.Browse.route) { BrowseScreen(sessionStore, nav) }
                composable(AppSection.Account.route) { AccountScreen(sessionStore, onSignedOut) }
                composable(
                    route = "aircraft/{reg}",
                    arguments = listOf(navArgument("reg") { type = NavType.StringType }),
                ) { entry ->
                    AircraftDetailScreen(registration = entry.arguments?.getString("reg").orEmpty(), onBack = { nav.popBackStack() })
                }
                composable(
                    route = "airline/{icao}",
                    arguments = listOf(navArgument("icao") { type = NavType.StringType }),
                ) { entry ->
                    AirlineDetailScreen(icao = entry.arguments?.getString("icao").orEmpty(), onBack = { nav.popBackStack() })
                }
                composable(
                    route = "airport/{ident}",
                    arguments = listOf(navArgument("ident") { type = NavType.StringType }),
                ) { entry ->
                    AirportDetailScreen(ident = entry.arguments?.getString("ident").orEmpty(), onBack = { nav.popBackStack() })
                }
                composable(
                    route = "callsign/{cs}",
                    arguments = listOf(navArgument("cs") { type = NavType.StringType }),
                ) { entry ->
                    CallsignDetailScreen(callsign = entry.arguments?.getString("cs").orEmpty(), onBack = { nav.popBackStack() })
                }
            }
        }
    }
}