package com.example.nutrimetrix.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
fun HomeScreen(
    onAddMeal: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is HomeUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        }
        is HomeUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text  = (uiState as HomeUiState.Error).message,
                    color = Color.Red
                )
            }
        }
        is HomeUiState.Success -> {
            val data = (uiState as HomeUiState.Success)
            HomeContent(
                firstName        = data.firstName,
                caloriasConsumidas = data.caloriasConsumidas,
                caloriasObjetivo = data.caloriasObjetivo,
                proteinasConsumidas  = data.proteinasConsumidas,
                proteinasObjetivo    = data.proteinasObjetivo,
                carbosConsumidos     = data.carbosConsumidos,
                carbosObjetivo       = data.carbosObjetivo,
                grasasConsumidas     = data.grasasConsumidas,
                grasasObjetivo       = data.grasasObjetivo,
                comidas          = data.comidas,
                onAddMeal        = onAddMeal
            )
        }
    }
}

@Composable
private fun HomeContent(
    firstName: String,
    caloriasConsumidas: Int,
    caloriasObjetivo: Int,
    proteinasConsumidas: Double,
    proteinasObjetivo: Double,
    carbosConsumidos: Double,
    carbosObjetivo: Double,
    grasasConsumidas: Double,
    grasasObjetivo: Double,
    comidas: List<ComidaResumen>,
    onAddMeal: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            // ── Saludo ────────────────────────────────────────────────────────
            Text(
                text       = "Hola, $firstName",
                fontSize   = 30.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.Black
            )

            Spacer(Modifier.height(20.dp))

            // ── Card de calorías ──────────────────────────────────────────────
            CaloriasCard(
                consumidas = caloriasConsumidas,
                objetivo   = caloriasObjetivo
            )

            Spacer(Modifier.height(28.dp))

            // ── Macronutrientes ───────────────────────────────────────────────
            Text("Macronutrientes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            MacrosCard(
                proteinasConsumidas = proteinasConsumidas,
                proteinasObjetivo   = proteinasObjetivo,
                carbosConsumidos    = carbosConsumidos,
                carbosObjetivo      = carbosObjetivo,
                grasasConsumidas    = grasasConsumidas,
                grasasObjetivo      = grasasObjetivo
            )

            Spacer(Modifier.height(28.dp))

            // ── Comidas del día ───────────────────────────────────────────────
            Text("Comidas de hoy", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            if (comidas.isEmpty()) {
                Text(
                    text     = "Aún no registraste comidas hoy",
                    color    = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                comidas.forEach { comida ->
                    ComidaItem(comida = comida)
                    Spacer(Modifier.height(4.dp))
                }
            }

            // Espacio para que el FAB no tape la última comida
            Spacer(Modifier.height(80.dp))
        }

        // ── FAB + ─────────────────────────────────────────────────────────────
        FloatingActionButton(
            onClick            = onAddMeal,
            containerColor     = GreenPrimary,
            contentColor       = Color.White,
            shape              = CircleShape,
            modifier           = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 80.dp)
        ) {
            Text(text = "+", fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
    }
}

// ── Card de calorías con gradiente ────────────────────────────────────────────
@Composable
private fun CaloriasCard(consumidas: Int, objetivo: Int) {
    val progreso = if (objetivo > 0) (consumidas.toFloat() / objetivo).coerceIn(0f, 1f) else 0f
    val restantes = (objetivo - consumidas).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(GreenLight, GreenDark)))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text     = "Calorias de hoy",
                color    = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text       = "$consumidas",
                    color      = Color.White,
                    fontSize   = 42.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text     = "/$objetivo kcal",
                    color    = Color.White.copy(alpha = 0.75f),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Barra de progreso
            LinearProgressIndicator(
                progress          = { progreso },
                modifier          = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color             = Color.White,
                trackColor        = Color.White.copy(alpha = 0.3f),
                strokeCap         = StrokeCap.Round
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text     = "Te quedan $restantes kcal para hoy",
                color    = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        }
    }
}

// ── Card de macros ────────────────────────────────────────────────────────────
@Composable
private fun MacrosCard(
    proteinasConsumidas: Double, proteinasObjetivo: Double,
    carbosConsumidos: Double,    carbosObjetivo: Double,
    grasasConsumidas: Double,    grasasObjetivo: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MacroRow(
                label     = "Proteinas",
                consumido = proteinasConsumidas,
                objetivo  = proteinasObjetivo,
                color     = Color(0xFFE53935)
            )
            Spacer(Modifier.height(12.dp))
            MacroRow(
                label     = "CarboH",
                consumido = carbosConsumidos,
                objetivo  = carbosObjetivo,
                color     = Color(0xFFFFA726)
            )
            Spacer(Modifier.height(12.dp))
            MacroRow(
                label     = "Grasas",
                consumido = grasasConsumidas,
                objetivo  = grasasObjetivo,
                color     = Color(0xFFFFD600)
            )
        }
    }
}

@Composable
private fun MacroRow(
    label: String,
    consumido: Double,
    objetivo: Double,
    color: Color
) {
    val progreso = if (objetivo > 0) (consumido / objetivo).toFloat().coerceIn(0f, 1f) else 0f

    Column {
        Row(
            modifier       = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                text     = "${"%.1f".format(consumido)}/${"%.1f".format(objetivo)}g",
                fontSize = 12.sp,
                color    = Color.Gray
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress   = { progreso },
            modifier   = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50)),
            color      = color,
            trackColor = Color(0xFFEEEEEE),
            strokeCap  = StrokeCap.Round
        )
    }
}

// ── Item de comida ────────────────────────────────────────────────────────────
@Composable
private fun ComidaItem(comida: ComidaResumen) {
    Row(
        modifier       = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícono circular verde con check
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(GreenPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = null,
                tint               = GreenPrimary,
                modifier           = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(comida.nombre, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                text     = comida.alimentos,
                fontSize = 12.sp,
                color    = Color.Gray
            )
        }

        Text(
            text       = "${comida.calorias}kcal",
            color      = GreenPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp
        )
    }

    HorizontalDivider(color = Color(0xFFF0F0F0))
}

// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    NutriMetrixTheme {
        HomeContent(
            firstName            = "Ezequiel",
            caloriasConsumidas   = 1650,
            caloriasObjetivo     = 2450,
            proteinasConsumidas  = 85.0,
            proteinasObjetivo    = 129.5,
            carbosConsumidos     = 180.0,
            carbosObjetivo       = 280.0,
            grasasConsumidas     = 45.0,
            grasasObjetivo       = 60.0,
            comidas = listOf(
                ComidaResumen("Desayuno",  "Avena, banana, almendras",   450),
                ComidaResumen("Almuerzo",  "Pollo, arroz, ensalada",     680),
                ComidaResumen("Merienda",  "Yogur, frutas",              220)
            ),
            onAddMeal = {}
        )
    }
}
