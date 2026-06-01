package com.fueltracker.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fueltracker.app.R

sealed class Screen(val route: String, val label: String, @DrawableRes val iconRes: Int) {
    data object Home : Screen("home", "Inicio", R.drawable.ic_home)
    data object Stations : Screen("stations", "Gasolineras", R.drawable.ic_local_gas_station)
    data object Analytics : Screen("analytics", "Análisis", R.drawable.ic_analytics)
    data object Settings : Screen("settings", "Ajustes", R.drawable.ic_settings)
}

val bottomNavScreens = listOf(Screen.Home, Screen.Stations, Screen.Analytics, Screen.Settings)

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavScreens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(painter = painterResource(screen.iconRes), contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
