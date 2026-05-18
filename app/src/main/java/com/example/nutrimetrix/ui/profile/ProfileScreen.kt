package com.example.nutrimetrix.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutrimetrix.ui.theme.GreenDark
import com.example.nutrimetrix.ui.theme.GreenLight
import com.example.nutrimetrix.ui.theme.GreenPrimary
import com.example.nutrimetrix.ui.theme.NutriMetrixTheme

@Composable
fun ProfileScreen(
    onAddMeal: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is ProfileUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        }
        is ProfileUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text((uiState as ProfileUiState.Error).message, color = Color.Red)
            }
        }
        is ProfileUiState.Success -> {
            val data = uiState as ProfileUiState.Success
            ProfileContent(
                peso           = data.peso,
                altura         = data.altura,
                nivelActividad = data.nivelActividad,
                pesoIdeal      = data.pesoIdeal,
                objetivo       = data.objetivo,
                semanasActual  = data.semanasTranscurridas,
                semanasTotal   = data.semanasTotal,
                onAddMeal      = onAddMeal
            )
        }
    }
}

@Composable
private fun ProfileContent(
    peso: Double,
    altura: Int,
    nivelActividad: String,
    pesoIdeal: Double,
    objetivo: String,
    semanasActual: Int,
    semanasTotal: Int,
    onAddMeal: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            Text(
                text       = "Mi perfil",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            // Avatar circular verde
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(GreenLight, GreenDark))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Person,
                    contentDescription = "Avatar",
                    tint               = Color.White,
                    modifier           = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text     = "Objetivo: ${objetivoLabel(objetivo)}",
                fontSize = 14.sp,
                color    = Color.Gray
            )

            Spacer(Modifier.height(32.dp))

            // Sección información personal
            Text(
                text       = "Informacion personal",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(16.dp))

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ProfileInfoRow(label = "Peso(kg)",  value = "${peso.toInt()}KG")
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileInfoRow(label = "Altura(cm)", value = "$altura cm")
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileInfoRow(label = "Actividad",  value = nivelActividadLabel(nivelActividad))
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileInfoRow(label = "Objetivo",   value = "${pesoIdeal.toInt()}kg")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Card progreso con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(GreenLight, GreenDark)))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text     = "Progreso para tu objetivo",
                        color    = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text       = "$semanasActual",
                            color      = Color.White,
                            fontSize   = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text     = "/$semanasTotal semanas",
                            color    = Color.White.copy(alpha = 0.8f),
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
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

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GreenPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = null,
                tint               = GreenPrimary,
                modifier           = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Helpers para labels legibles ──────────────────────────────────────────────
private fun objetivoLabel(objetivo: String) = when (objetivo) {
    "DEFICIT"       -> "Pérdida de peso"
    "SUPERAVIT"     -> "Ganancia de peso"
    "MANTENIMIENTO" -> "Mantenimiento"
    else            -> objetivo
}

private fun nivelActividadLabel(nivel: String) = when (nivel) {
    "SEDENTARIO" -> "Sedentario"
    "LIGERO"     -> "Ligero 1-3 días"
    "MODERADO"   -> "Modera 3-5 días"
    "ACTIVO"     -> "Activo 6-7 días"
    "MUY_ACTIVO" -> "Muy activo"
    else         -> nivel
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    NutriMetrixTheme {
        ProfileContent(
            peso           = 70.0,
            altura         = 175,
            nivelActividad = "MODERADO",
            pesoIdeal      = 65.0,
            objetivo       = "DEFICIT",
            semanasActual  = 4,
            semanasTotal   = 12,
            onAddMeal      = {}
        )
    }
}
