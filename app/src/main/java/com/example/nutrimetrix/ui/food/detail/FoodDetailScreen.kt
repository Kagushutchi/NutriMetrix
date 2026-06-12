package com.example.nutrimetrix.ui.food.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.nutrimetrix.ui.theme.GreenDark
import com.example.nutrimetrix.ui.theme.GreenLight
import com.example.nutrimetrix.ui.theme.GreenPrimary
import com.example.nutrimetrix.ui.theme.NutriMetrixTheme

@Composable
fun FoodDetailScreen(
    onBack:    () -> Unit,
    viewModel: FoodDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is FoodDetailUiState.Loading -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        }
        is FoodDetailUiState.Error -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text((uiState as FoodDetailUiState.Error).message, color = Color.Red)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onBack) { Text("Volver") }
                }
            }
        }
        is FoodDetailUiState.Success -> {
            FoodDetailContent(
                comida = (uiState as FoodDetailUiState.Success).comida,
                onBack = onBack
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun FoodDetailContent(comida: ComidaDetalle, onBack: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // ── Header con gradiente o imagen ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            // Fondo dinámico: Imagen o Gradiente original
            if (comida.imageUrl.isNotBlank()) {
                GlideImage(
                    model = comida.imageUrl,
                    contentDescription = "Imagen de la comida",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Sombreado para que el texto blanco contraste bien
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 100f
                            )
                        )
                )
            } else {
                // Gradiente original naranja/rojo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFF8C00), Color(0xFFFF3B30))
                            )
                        )
                )
            }

            // Botón volver
            IconButton(
                onClick  = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint               = Color.White
                )
            }

            // Info en el header
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                // Fecha y hora
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint     = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(comida.fecha, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint     = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(comida.hora, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = comida.tipo.replaceFirstChar { it.uppercase() }.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    color      = Color.White,
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${"%.0f".format(comida.totalKcal)} kcal totales",
                    color    = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp
                )
            }
        }

        // ── Contenido ─────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            Spacer(Modifier.height(24.dp))

            // Alimentos incluidos
            Text("Alimentos", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // Cada alimento separado por coma en su propia fila
            comida.nombre.split(",").map { it.trim() }.forEach { alimento ->
                if (alimento.isNotBlank()) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(GreenPrimary)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(alimento, fontSize = 15.sp)
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Card de calorías
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(GreenLight, GreenDark)))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Calorías totales", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Text(
                        "${"%.0f".format(comida.totalKcal)}",
                        color      = Color.White,
                        fontSize   = 44.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("kcal", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Macros
            Text("Macronutrientes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacroDetailCard(
                    label    = "Proteínas",
                    value    = comida.proteinas,
                    color    = Color(0xFFE53935),
                    modifier = Modifier.weight(1f)
                )
                MacroDetailCard(
                    label    = "Carbos",
                    value    = comida.carbos,
                    color    = Color(0xFFFFA726),
                    modifier = Modifier.weight(1f)
                )
                MacroDetailCard(
                    label    = "Grasas",
                    value    = comida.grasas,
                    color    = Color(0xFFFFD600),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Barras de distribución
            Text("Distribución", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MacroBarRow("Proteínas", comida.proteinas, comida.totalKcal / 4, Color(0xFFE53935))
                    Spacer(Modifier.height(12.dp))
                    MacroBarRow("Carbos",    comida.carbos,    comida.totalKcal / 4, Color(0xFFFFA726))
                    Spacer(Modifier.height(12.dp))
                    MacroBarRow("Grasas",    comida.grasas,    comida.totalKcal / 9, Color(0xFFFFD600))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MacroDetailCard(label: String, value: Double, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${"%.1f".format(value)}g", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun MacroBarRow(label: String, valor: Double, max: Double, color: Color) {
    val progreso = if (max > 0) (valor / max).toFloat().coerceIn(0f, 1f) else 0f
    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("${"%.1f".format(valor)}g", fontSize = 12.sp, color = Color.Gray)
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FoodDetailPreview() {
    NutriMetrixTheme {
        FoodDetailContent(
            comida = ComidaDetalle(
                id        = "1",
                tipo      = "ALMUERZO",
                nombre    = "Pechuga de pollo, Arroz blanco, Ensalada mixta",
                totalKcal = 450.0,
                proteinas = 38.0,
                carbos    = 42.0,
                grasas    = 8.0,
                fecha     = "12 Abril 2026",
                hora      = "13:00",
                imageUrl  = ""
            ),
            onBack = {}
        )
    }
}