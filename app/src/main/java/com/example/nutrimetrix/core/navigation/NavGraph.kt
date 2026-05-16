package com.example.nutrimetrix.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Rutas de la aplicación.
 * Usar sealed class evita strings sueltos por toda la app.
 */
sealed class Screen(val route: String) {
    object Login    : Screen("login")
    object Register : Screen("register")
    object Home     : Screen("home")
    object FoodList : Screen("food_list")
    object FoodDetail : Screen("food_detail/{foodId}") {
        fun createRoute(foodId: String) = "food_detail/$foodId"
    }
    object Camera   : Screen("camera")
    object Profile  : Screen("profile")
    object Gallery  : Screen("gallery")
    object Settings : Screen("settings")
}

/**
 * Grafo de navegación principal.
 * Por ahora tiene placeholders para cada pantalla —
 * se irán completando en cada rama/feature.
 *
 * @param startDestination destino inicial determinado por [SplashViewModel]
 */
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(Screen.Login.route) {

            PlaceholderScreen("Login")
        }

        composable(Screen.Register.route) {

            PlaceholderScreen("Register")
        }

        composable(Screen.Home.route) {

            PlaceholderScreen("Home")
        }

        composable(Screen.FoodList.route) {

            PlaceholderScreen("Food List")
        }

        composable(Screen.FoodDetail.route) {

            PlaceholderScreen("Food Detail")
        }

        composable(Screen.Camera.route) {

            PlaceholderScreen("Camera")
        }

        composable(Screen.Profile.route) {
            PlaceholderScreen("Profile")
        }

        composable(Screen.Gallery.route) {
            PlaceholderScreen("Gallery")
        }

        composable(Screen.Settings.route) {
            PlaceholderScreen("Settings")
        }
    }
}

// Composable temporal para no dejar composables vacíos
@Composable
private fun PlaceholderScreen(name: String) {
    androidx.compose.material3.Text(text = "Pantalla: $name — en construcción")
}
