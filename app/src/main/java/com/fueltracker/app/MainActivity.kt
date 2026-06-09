package com.fueltracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.fueltracker.app.domain.model.ThemeMode
import com.fueltracker.app.ui.MainViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fueltracker.app.ui.navigation.BottomNavigationBar
import com.fueltracker.app.ui.navigation.Screen
import com.fueltracker.app.ui.screens.analytics.AnalyticsScreen
import com.fueltracker.app.ui.screens.home.HomeScreen
import com.fueltracker.app.ui.screens.settings.SettingsScreen
import com.fueltracker.app.ui.screens.stations.StationsScreen
import com.fueltracker.app.ui.theme.FuelTrackerTheme
import com.fueltracker.app.worker.PriceSyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var priceSyncScheduler: PriceSyncScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            FuelTrackerTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavigationBar(navController) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) { HomeScreen() }
                        composable(Screen.Stations.route) { StationsScreen() }
                        composable(Screen.Analytics.route) { AnalyticsScreen() }
                        composable(Screen.Settings.route) { SettingsScreen() }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            priceSyncScheduler.enqueueSyncIfStale()
        }
    }
}
