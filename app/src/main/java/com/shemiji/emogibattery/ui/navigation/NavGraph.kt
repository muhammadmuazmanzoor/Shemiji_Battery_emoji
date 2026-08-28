package com.shemiji.emogibattery.ui.navigation

import android.net.Uri
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shemiji.emogibattery.system.AccessibilityPermission
import com.shemiji.emogibattery.ui.screens.AccessibilityOnboardingScreen
import com.shemiji.emogibattery.ui.screens.BatteryCustomizationScreen
import com.shemiji.emogibattery.ui.screens.HomeScreen
import com.shemiji.emogibattery.ui.screens.SettingsScreen
import com.shemiji.emogibattery.ui.screens.ShimejiScreen
import com.shemiji.emogibattery.ui.screens.ShimejiDetailScreen
import com.shemiji.emogibattery.ui.screens.WallpapersScreen
import com.shemiji.emogibattery.ui.viewmodel.ThemeViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object BatteryCustomization : Screen("battery-customization")
    data object Wallpapers : Screen("wallpapers")
    data object Shimeji : Screen("shimeji")
    data object ShimejiDetail : Screen("shimeji-detail/{characterId}") {
        fun createRoute(characterId: String) = "shimeji-detail/${Uri.encode(characterId)}"
    }
    data object Settings : Screen("settings")
    data object AccessibilityOnboarding : Screen("accessibility-onboarding")
}

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    var accessibilityEnabled by remember {
        mutableStateOf(AccessibilityPermission.isEnabled(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = AccessibilityPermission.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                onShimejiCharacter = { characterId ->
                    navController.navigate(Screen.ShimejiDetail.createRoute(characterId))
                },
                onSettings = { navController.navigate(Screen.Settings.route) },
                showAccessibilityPermission = !accessibilityEnabled,
                onAccessibilityAgree = {
                    navController.navigate(Screen.AccessibilityOnboarding.route)
                },
            )
        }
        composable(Screen.AccessibilityOnboarding.route) {
            LaunchedEffect(accessibilityEnabled) {
                if (accessibilityEnabled) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            AccessibilityOnboardingScreen(
                onBack = { navController.navigateUp() },
                onOpenSettings = { AccessibilityPermission.openSettings(context) },
            )
        }
        composable(Screen.BatteryCustomization.route) {
            BatteryCustomizationScreen(onBack = { navController.navigateUp() })
        }
        composable(Screen.Wallpapers.route) {
            WallpapersScreen(onBack = { navController.navigateUp() })
        }
        composable(Screen.Shimeji.route) {
            ShimejiScreen(
                onBack = { navController.navigateUp() },
                onCharacterClick = { characterId ->
                    navController.navigate(Screen.ShimejiDetail.createRoute(characterId))
                },
            )
        }
        composable(Screen.ShimejiDetail.route) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId").orEmpty()
            ShimejiDetailScreen(
                characterId = characterId,
                onBack = { navController.navigateUp() },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                themeViewModel = themeViewModel,
                onBack = { navController.navigateUp() },
            )
        }
    }
}
