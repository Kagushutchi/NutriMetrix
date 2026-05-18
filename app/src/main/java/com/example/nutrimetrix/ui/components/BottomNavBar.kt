package com.example.nutrimetrix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nutrimetrix.core.navigation.Screen
import com.example.nutrimetrix.ui.theme.GreenPrimary
import com.example.nutrimetrix.ui.theme.NutriMetrixTheme

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val isCentral: Boolean = false
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route,    Icons.Default.Home,      "Inicio"),
    BottomNavItem(Screen.Profile.route, Icons.Default.Person,    "Perfil"),
    BottomNavItem(Screen.Camera.route,  Icons.Default.CameraAlt, "Cámara", isCentral = true),
    BottomNavItem(Screen.Gallery.route, Icons.Default.Image,     "Galería"),
    BottomNavItem(Screen.Settings.route,Icons.Default.Settings,  "Ajustes")
)

@Composable
fun BottomNavBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier       = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        bottomNavItems.forEach { item ->
            if (item.isCentral) {

                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick  = {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = item.icon,
                                contentDescription = item.label,
                                tint               = Color.White,
                                modifier           = Modifier.size(26.dp)
                            )
                        }
                    },
                    label  = null,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            } else {
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick  = {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector        = item.icon,
                            contentDescription = item.label,
                            modifier           = Modifier.size(24.dp)
                        )
                    },
                    label  = null,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = GreenPrimary,
                        unselectedIconColor = Color.Gray,
                        indicatorColor      = Color.Transparent
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavBarPreview() {
    NutriMetrixTheme {
        BottomNavBar(navController = rememberNavController())
    }
}
