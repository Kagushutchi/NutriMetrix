package com.example.nutrimetrix.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutrimetrix.ui.theme.GreenPrimary
import com.example.nutrimetrix.ui.theme.NutriMetrixTheme

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onAddMeal: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Cuando el logout es exitoso, navegamos al Login
    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.LoggedOut) {
            onLogout()
        }
    }

    // Diálogo de confirmación
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title   = { Text("Cerrar sesión") },
            text    = { Text("¿Estás seguro que querés cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        viewModel.cerrarSesion()
                    }
                ) {
                    Text("Cerrar sesión", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar", color = GreenPrimary)
                }
            }
        )
    }

    SettingsContent(
        isLoading     = uiState is SettingsUiState.Loading,
        onLogoutClick = { showDialog = true },
        onAddMeal     = onAddMeal
    )
}

@Composable
private fun SettingsContent(
    isLoading: Boolean,
    onLogoutClick: () -> Unit,
    onAddMeal: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            Text(
                text       = "Configuracion",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(32.dp))

            // Botón cerrar sesión con borde rojo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, Color(0xFFE53935), RoundedCornerShape(14.dp))
                    .clickable(enabled = !isLoading) { onLogoutClick() }
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícono
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint               = Color(0xFFE53935),
                            modifier           = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    if (isLoading) {
                        CircularProgressIndicator(
                            color    = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text       = "Cerrar sesion",
                            color      = Color(0xFFE53935),
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 15.sp,
                            modifier   = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint               = Color(0xFFE53935)
                        )
                    }
                }
            }
        }

        // FAB +
        FloatingActionButton(
            onClick        = onAddMeal,
            containerColor = GreenPrimary,
            contentColor   = Color.White,
            shape          = CircleShape,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 80.dp)
        ) {
            Text("+", fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    NutriMetrixTheme {
        SettingsContent(isLoading = false, onLogoutClick = {}, onAddMeal = {})
    }
}
