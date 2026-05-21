package com.example.nutrimetrix.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nutrimetrix.ui.auth.AuthViewModel
import com.example.nutrimetrix.ui.auth.login.LoginScreen
import com.example.nutrimetrix.ui.auth.register.RegisterResultScreen
import com.example.nutrimetrix.ui.auth.register.RegisterStep2Screen
import com.example.nutrimetrix.ui.auth.register.RegisterStep3Screen
import com.example.nutrimetrix.ui.auth.register.RegisterStep4Screen
import com.example.nutrimetrix.ui.components.BottomNavBar
import com.example.nutrimetrix.ui.food.list.FoodListScreen
import com.example.nutrimetrix.ui.home.HomeScreen
import com.example.nutrimetrix.ui.profile.ProfileScreen
import com.example.nutrimetrix.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object Register       : Screen("register_step2")
    object RegisterStep3  : Screen("register_step3")
    object RegisterStep4  : Screen("register_step4")
    object RegisterResult : Screen("register_result")
    object Home           : Screen("home")
    object FoodList       : Screen("food_list")
    object FoodDetail     : Screen("food_detail/{foodId}") {
        fun createRoute(foodId: String) = "food_detail/$foodId"
    }
    object Camera         : Screen("camera")
    object Profile        : Screen("profile")
    object Gallery        : Screen("gallery")
    object Settings       : Screen("settings")
}

// Rutas donde se muestra la BottomNavBar
private val bottomBarRoutes = setOf(
    Screen.Home.route,
    Screen.Profile.route,
    Screen.Camera.route,
    Screen.Gallery.route,
    Screen.Settings.route
)

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String
) {
    val sharedAuthViewModel: AuthViewModel = hiltViewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route
    val showBottomBar  = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding)
        ) {

            // ── Auth ──────────────────────────────────────────────────────────
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel      = sharedAuthViewModel,
                    onLoginSuccess = { isNewUser ->
                        if (isNewUser) {
                            navController.navigate(Screen.Register.route)
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterStep2Screen(
                    viewModel = sharedAuthViewModel,
                    onNext    = { navController.navigate(Screen.RegisterStep3.route) }
                )
            }

            composable(Screen.RegisterStep3.route) {
                RegisterStep3Screen(
                    viewModel = sharedAuthViewModel,
                    onNext    = { navController.navigate(Screen.RegisterStep4.route) }
                )
            }

            composable(Screen.RegisterStep4.route) {
                RegisterStep4Screen(
                    viewModel = sharedAuthViewModel,
                    onNext    = { navController.navigate(Screen.RegisterResult.route) }
                )
            }

            composable(Screen.RegisterResult.route) {
                RegisterResultScreen(
                    viewModel = sharedAuthViewModel,
                    onFinish  = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Pantallas principales (con BottomNavBar) ──────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddMeal = { navController.navigate(Screen.FoodList.route) }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onAddMeal = { navController.navigate(Screen.FoodList.route) }
                )
            }
            composable(Screen.Camera.route)   { PlaceholderScreen("Cámara") }
            composable(Screen.Gallery.route)  { PlaceholderScreen("Galería") }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onAddMeal = { navController.navigate(Screen.FoodList.route) },
                    onLogout  = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.FoodList.route) {
                FoodListScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.FoodDetail.route) { PlaceholderScreen("Detalle alimento") }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    androidx.compose.material3.Text(text = "Pantalla: $name — en construcción")
}
