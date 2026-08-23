package com.shemiji.emogibattery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shemiji.emogibattery.ui.screens.BatteryCustomizationScreen
import com.shemiji.emogibattery.ui.screens.HomeScreen
import com.shemiji.emogibattery.ui.screens.SettingsScreen
import com.shemiji.emogibattery.ui.screens.ShimejiScreen
import com.shemiji.emogibattery.ui.screens.WallpapersScreen
import com.shemiji.emogibattery.ui.viewmodel.ThemeViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object BatteryCustomization : Screen("battery-customization")
    data object Wallpapers : Screen("wallpapers")
    data object Shimeji : Screen("shimeji")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onBatteryCustomization = {
                    navController.navigate(Screen.BatteryCustomization.route)
                },
                onWallpapers = { navController.navigate(Screen.Wallpapers.route) },
                onShimeji = { navController.navigate(Screen.Shimeji.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
            )
        }
        composable(Screen.BatteryCustomization.route) {
            BatteryCustomizationScreen(onBack = { navController.navigateUp() })
        }
        composable(Screen.Wallpapers.route) {
            WallpapersScreen(onBack = { navController.navigateUp() })
        }
        composable(Screen.Shimeji.route) {
            ShimejiScreen(onBack = { navController.navigateUp() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                themeViewModel = themeViewModel,
                onBack = { navController.navigateUp() },
            )
        }
    }
}
