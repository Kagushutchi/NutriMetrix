package com.example.nutrimetrix.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
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
    onAddMeal:     () -> Unit,
    onComidaClick: (String) -> Unit = {},
    viewModel:     HomeViewModel = hiltViewModel()
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
                Text((uiState as HomeUiState.Error).message, color = Color.Red)
            }
        }
        is HomeUiState.Success -> {
            val data = uiState as HomeUiState.Success
            HomeContent(
                firstName           = data.firstName,
                caloriasConsumidas  = data.caloriasConsumidas,
                caloriasObjetivo    = data.caloriasObjetivo,
                proteinasConsumidas = data.proteinasConsumidas,
                proteinasObjetivo   = data.proteinasObjetivo,
                carbosConsumidos    = data.carbosConsumidos,
                carbosObjetivo      = data.carbosObjetivo,
                grasasConsumidas    = data.grasasConsumidas,
                grasasObjetivo      = data.grasasObjetivo,
                comidas             = data.comidas,
                onAddMeal           = onAddMeal,
                onComidaClick       = onComidaClick
            )
        }
    }
}

// Asegúrate de tener estos imports en tu HomeScreen.kt


@Composable
private fun HomeContent(
    firstName:          String,
    caloriasConsumidas: Int,
    caloriasObjetivo:   Int,
    proteinasConsumidas:Double,
    proteinasObjetivo:  Double,
    carbosConsumidos:   Double,
    carbosObjetivo:     Double,
    grasasConsumidas:   Double,
    grasasObjetivo:     Double,
    comidas:            List<ComidaResumen>,
    onAddMeal:          () -> Unit,
    onComidaClick:      (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            // Mover los Spacers superior e inferior al contentPadding es una buena práctica en LazyColumn
            contentPadding = PaddingValues(top = 52.dp, bottom = 80.dp)
        ) {

            // 1. Contenido estático: Saludo y Calorías
            item {
                Text("Hola, $firstName", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                CaloriasCard(consumidas = caloriasConsumidas, objetivo = caloriasObjetivo)
                Spacer(Modifier.height(28.dp))
            }

            // 2. Contenido estático: Macros
            item {
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
            }

            // 3. Título de Comidas
            item {
                Text("Comidas de hoy", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
            }

            // 4. Lista Dinámica
            if (comidas.isEmpty()) {
                item {
                    Text("Aún no registraste comidas hoy", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                // Usamos 'items' en lugar de un 'forEach'
                items(
                    items = comidas,
                    key = { comida -> comida.id } // Usar un 'key' mejora muchísimo el rendimiento de Compose
                ) { comida ->
                    ComidaItem(
                        comida  = comida,
                        onClick = { onComidaClick(comida.id) }
                    )
                }
            }
        }

        // El FloatingActionButton se mantiene igual, flotando por encima del LazyColumn
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
private fun CaloriasCard(consumidas: Int, objetivo: Int) {
    val progreso  = if (objetivo > 0) (consumidas.toFloat() / objetivo).coerceIn(0f, 1f) else 0f
    val restantes = (objetivo - consumidas).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(GreenLight, GreenDark)))
            .padding(20.dp)
    ) {
        Column {
            Text("Calorias de hoy", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$consumidas", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Text("/$objetivo kcal", color = Color.White.copy(alpha = 0.75f), fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress  = { progreso },
                modifier  = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color     = Color.White,
                trackColor= Color.White.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(10.dp))
            Text("Te quedan $restantes kcal para hoy", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun MacrosCard(
    proteinasConsumidas: Double, proteinasObjetivo: Double,
    carbosConsumidos: Double,    carbosObjetivo: Double,
    grasasConsumidas: Double,    grasasObjetivo: Double
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MacroRow("Proteinas", proteinasConsumidas, proteinasObjetivo, Color(0xFFE53935))
            Spacer(Modifier.height(12.dp))
            MacroRow("CarboH",    carbosConsumidos,    carbosObjetivo,    Color(0xFFFFA726))
            Spacer(Modifier.height(12.dp))
            MacroRow("Grasas",    grasasConsumidas,    grasasObjetivo,    Color(0xFFFFD600))
        }
    }
}

@Composable
private fun MacroRow(label: String, consumido: Double, objetivo: Double, color: Color) {
    val progreso = if (objetivo > 0) (consumido / objetivo).toFloat().coerceIn(0f, 1f) else 0f
    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${"%.1f".format(consumido)}/${"%.1f".format(objetivo)}g", fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress   = { progreso },
            modifier   = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
            color      = color,
            trackColor = Color(0xFFEEEEEE),
            strokeCap  = StrokeCap.Round
        )
    }
}

@Composable
private fun ComidaItem(comida: ComidaResumen, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(GreenPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null,
                tint = GreenPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(comida.nombre,    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(comida.alimentos, fontSize   = 12.sp, color = Color.Gray)
        }
        Text("${comida.calorias}kcal", color = GreenPrimary,
            fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Icon(Icons.Default.ChevronRight, contentDescription = null,
            tint = Color.LightGray, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = Color(0xFFF0F0F0))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    NutriMetrixTheme {
        HomeContent(
            firstName           = "Ezequiel",
            caloriasConsumidas  = 1650,
            caloriasObjetivo    = 2450,
            proteinasConsumidas = 85.0,
            proteinasObjetivo   = 129.5,
            carbosConsumidos    = 180.0,
            carbosObjetivo      = 280.0,
            grasasConsumidas    = 45.0,
            grasasObjetivo      = 60.0,
            comidas = listOf(
                ComidaResumen("1", "Desayuno", "Avena, banana, almendras", 450),
                ComidaResumen("2", "Almuerzo", "Pollo, arroz, ensalada",  680),
                ComidaResumen("3", "Merienda", "Yogur, frutas",           220)
            ),
            onAddMeal     = {},
            onComidaClick = {}
        )
    }
}
