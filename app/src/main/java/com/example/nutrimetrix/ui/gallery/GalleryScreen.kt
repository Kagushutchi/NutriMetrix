package com.example.nutrimetrix.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.nutrimetrix.ui.theme.GreenPrimary
import com.example.nutrimetrix.ui.theme.NutriMetrixTheme

// Gradientes para las cards — se ciclan cuando no hay imagen
private val cardGradients = listOf(
    listOf(Color(0xFFFF8C00), Color(0xFFFF3B30)),  // naranja → rojo
    listOf(Color(0xFFFF6B35), Color(0xFFE53935)),  // naranja claro → rojo
    listOf(Color(0xFFFF9A3C), Color(0xFFFF4444)),  // durazno → rojo
    listOf(Color(0xFFFFB347), Color(0xFFFF6B6B)),  // amarillo → rosa
)

@Composable
fun GalleryScreen(
    onComidaClick: (String) -> Unit,
    onAddMeal:     () -> Unit,
    viewModel:     GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.cargarGaleria()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            Text("Galeria",               fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Historial de comidas",  fontSize = 14.sp, color = Color.Gray)

            Spacer(Modifier.height(20.dp))

            when (uiState) {
                is GalleryUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary)
                    }
                }
                is GalleryUiState.Error -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text((uiState as GalleryUiState.Error).message, color = Color.Red)
                    }
                }
                is GalleryUiState.Success -> {
                    val comidas = (uiState as GalleryUiState.Success).comidas

                    if (comidas.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Aún no registraste comidas",
                                    color    = Color.Gray,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "Usá el botón + para agregar",
                                    color    = Color.LightGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns               = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement   = Arrangement.spacedBy(12.dp),
                            contentPadding        = PaddingValues(bottom = 100.dp)
                        ) {
                            items(
                                items = comidas,
                                key   = { it.id }
                            ) { comida ->
                                ComidaGaleriaCard(
                                    comida    = comida,
                                    gradiente = cardGradients[comidas.indexOf(comida) % cardGradients.size],
                                    onClick   = { onComidaClick(comida.id) }
                                )
                            }
                        }
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

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ComidaGaleriaCard(
    comida:    ComidaGaleria,
    gradiente: List<Color>,
    onClick:   () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        // ── Fondo de la tarjeta (Imagen o Gradiente) ──
        if (comida.imageUrl.isNotBlank()) {
            // Renderizamos la imagen con Glide
            GlideImage(
                model = comida.imageUrl,
                contentDescription = "Imagen de ${comida.tipo}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay oscuro para que el texto blanco contraste bien sobre la foto
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 150f
                        )
                    )
            )
        } else {
            // Fallback al gradiente si no hay URL cargada
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(gradiente))
            )
        }

        // ── Contenido de texto superpuesto ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Fecha y hora
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint     = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    comida.fecha,
                    color    = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint     = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    comida.hora,
                    color    = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            // Tipo de comida
            Text(
                text       = comida.tipo.replaceFirstChar { it.uppercase() }.lowercase()
                    .replaceFirstChar { it.uppercase() },
                color      = Color.White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Calorías
            Text(
                text     = "${"%.0f".format(comida.totalKcal)}kcal",
                color    = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GalleryScreenPreview() {
    NutriMetrixTheme {
        val previewComidas = listOf(
            ComidaGaleria("1", "DESAYUNO",  "Avena, banana",  220.0, 8.0,  40.0, 4.0,  "12 Abril", "09:30", "https://i.ibb.co/example.jpg"), // Simulación con URL
            ComidaGaleria("2", "CENA",      "Pollo, ensalada",500.0, 35.0, 20.0, 18.0, "12 Abril", "22:00", ""),
            ComidaGaleria("3", "ALMUERZO",  "Arroz, carne",   450.0, 30.0, 55.0, 10.0, "12 Abril", "13:00", ""),
            ComidaGaleria("4", "MERIENDA",  "Yogur, frutas",  300.0, 10.0, 45.0, 5.0,  "12 Abril", "17:00", ""),
            ComidaGaleria("5", "CENA",      "Fideos",         600.0, 20.0, 80.0, 12.0, "11 Abril", "21:30", ""),
        )
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp)) {
                Spacer(Modifier.height(52.dp))
                Text("Galeria",              fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Historial de comidas", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(20.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    contentPadding        = PaddingValues(bottom = 100.dp)
                ) {
                    items(previewComidas) { comida ->
                        ComidaGaleriaCard(
                            comida    = comida,
                            gradiente = cardGradients[previewComidas.indexOf(comida) % cardGradients.size],
                            onClick   = {}
                        )
                    }
                }
            }
        }
    }
}