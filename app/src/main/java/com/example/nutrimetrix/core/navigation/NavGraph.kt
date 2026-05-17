package com.example.nutrimetrix.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nutrimetrix.ui.auth.login.LoginScreen
import com.example.nutrimetrix.ui.auth.register.RegisterResultScreen
import com.example.nutrimetrix.ui.auth.register.RegisterStep2Screen
import com.example.nutrimetrix.ui.auth.register.RegisterStep3Screen
import com.example.nutrimetrix.ui.auth.register.RegisterStep4Screen

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

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
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
                onNext = { navController.navigate(Screen.RegisterStep3.route) }
            )
        }

        composable(Screen.RegisterStep3.route) {
            RegisterStep3Screen(
                onNext = { navController.navigate(Screen.RegisterStep4.route) }
            )
        }

        composable(Screen.RegisterStep4.route) {
            RegisterStep4Screen(
                onNext = { navController.navigate(Screen.RegisterResult.route) }
            )
        }

        composable(Screen.RegisterResult.route) {
            RegisterResultScreen(
                onFinish = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            PlaceholderScreen("Home")
        }
        composable(Screen.FoodList.route)   { PlaceholderScreen("Food List") }
        composable(Screen.FoodDetail.route) { PlaceholderScreen("Food Detail") }
        composable(Screen.Camera.route)     { PlaceholderScreen("Camera") }
        composable(Screen.Profile.route)    { PlaceholderScreen("Profile") }
        composable(Screen.Gallery.route)    { PlaceholderScreen("Gallery") }
        composable(Screen.Settings.route)   { PlaceholderScreen("Settings") }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    androidx.compose.material3.Text(text = "Pantalla: $name — en construcción")
}
