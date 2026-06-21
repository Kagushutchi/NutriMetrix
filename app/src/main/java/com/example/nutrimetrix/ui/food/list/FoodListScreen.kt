package com.example.nutrimetrix.ui.food.list


import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.nutrimetrix.domain.model.Alimento
import com.example.nutrimetrix.ui.theme.GreenPrimary
import com.example.nutrimetrix.ui.theme.NutriMetrixTheme

val tiposDeComida = listOf("Colación", "Desayuno", "Almuerzo", "Merienda", "Cena")

@Composable
fun FoodListScreen(
    onNavigateBack: () -> Unit,
    viewModel: FoodListViewModel = hiltViewModel()
) {
    val query       by viewModel.query.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val cart        by viewModel.cart.collectAsStateWithLifecycle()
    val tipoComida  by viewModel.tipoComida.collectAsStateWithLifecycle()
    val saveState   by viewModel.saveState.collectAsStateWithLifecycle()

    // Navegar de vuelta cuando se guarda exitosamente
    LaunchedEffect(saveState) {
        if (saveState is SaveUiState.Success) {
            viewModel.resetSaveState()
            onNavigateBack()
        }
    }

    // Sheet del carrito
    var showCart by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            // ── Encabezado ────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Arma tu platillo",
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Crea tu plato",
                        fontSize = 13.sp,
                        color    = Color.Gray
                    )
                }

                // Ícono del carrito con badge
                BadgedBox(
                    badge = {
                        if (cart.isNotEmpty()) {
                            Badge(containerColor = GreenPrimary) {
                                Text("${cart.size}", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                ) {
                    IconButton(onClick = { showCart = true }) {
                        Icon(
                            imageVector        = Icons.Default.ShoppingCart,
                            contentDescription = "Carrito",
                            tint               = if (cart.isNotEmpty()) GreenPrimary else Color.Gray
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Dropdown tipo de comida ───────────────────────────────────────
            TipoComidaDropdown(
                selected  = tipoComida,
                onSelect  = { viewModel.onTipoComidaChange(it) }
            )

            Spacer(Modifier.height(16.dp))

            // ── Barra de búsqueda ─────────────────────────────────────────────
            OutlinedTextField(
                value         = query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder   = { Text("Buscar alimento...", color = Color.LightGray) },
                leadingIcon   = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = GreenPrimary)
                },
                trailingIcon  = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = GreenPrimary,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(Modifier.height(12.dp))

            // Contador de resultados
            val resultCount = if (searchState is SearchUiState.Success)
                (searchState as SearchUiState.Success).results.size else 0

            Row(
                modifier       = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Tipo de comida",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$resultCount items",
                    fontSize = 13.sp,
                    color    = Color.Gray
                )
            }

            Spacer(Modifier.height(12.dp))

            //Lista de resultados
            when (searchState) {
                is SearchUiState.Idle -> {
                    Box(Modifier.fillMaxWidth().padding(top = 32.dp), Alignment.Center) {
                        Text("Buscá un alimento para comenzar", color = Color.Gray, fontSize = 14.sp)
                    }
                }
                is SearchUiState.Loading -> {
                    Box(Modifier.fillMaxWidth().padding(top = 32.dp), Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary)
                    }
                }
                is SearchUiState.Error -> {
                    Text(
                        (searchState as SearchUiState.Error).message,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                is SearchUiState.Success -> {
                    val alimentos = (searchState as SearchUiState.Success).results
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding      = PaddingValues(bottom = 100.dp)
                    ) {
                        items(
                            items = alimentos,
                            key   = { it.fdcId }
                        ) { alimento ->
                            val enCarrito = cart.any { it.alimento.fdcId == alimento.fdcId }
                            AlimentoCard(
                                alimento  = alimento,
                                enCarrito = enCarrito,
                                onAdd     = { viewModel.addToCart(alimento) },
                                onRemove  = { viewModel.removeFromCart(alimento.fdcId) }
                            )
                        }
                    }
                }
            }
        }

        // ── Botón confirmar flotante ──────────────────────────────────────────
        if (cart.isNotEmpty() && tipoComida.isNotEmpty()) {
            val isLoading = saveState is SaveUiState.Loading
            ExtendedFloatingActionButton(
                onClick            = { viewModel.confirmarComida(onNavigateBack) },
                containerColor     = GreenPrimary,
                contentColor       = Color.White,
                modifier           = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirmar (${cart.size} items)", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // ── Bottom sheet del carrito ──────────────────────────────────────────────
    if (showCart) {
        CartBottomSheet(
            cart     = cart,
            onRemove = { viewModel.removeFromCart(it) },
            onClose  = { showCart = false }
        )
    }
}

// ── Dropdown de tipo de comida ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TipoComidaDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded         = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value         = selected.ifEmpty { "Seleccionar tipo de comida" },
            onValueChange = {},
            readOnly      = true,
            trailingIcon  = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier      = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape         = RoundedCornerShape(14.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = GreenPrimary,
                unfocusedBorderColor = if (selected.isEmpty()) Color.Red.copy(alpha = 0.5f)
                                       else Color.LightGray
            ),
            isError       = selected.isEmpty(),
            supportingText = if (selected.isEmpty()) {
                { Text("Seleccioná el tipo de comida para poder registrar", fontSize = 11.sp, color = Color.Red) }
            } else null
        )

        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false }
        ) {
            tiposDeComida.forEach { tipo ->
                DropdownMenuItem(
                    text    = { Text(tipo) },
                    onClick = {
                        onSelect(tipo.uppercase())
                        expanded = false
                    },
                    leadingIcon = {
                        if (selected == tipo.uppercase()) {
                            Icon(Icons.Default.Check, null, tint = GreenPrimary)
                        }
                    }
                )
            }
        }
    }
}

// ── Card de un alimento en los resultados ─────────────────────────────────────
@Composable
private fun AlimentoCard(
    alimento: Alimento,
    enCarrito: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (enCarrito) GreenPrimary.copy(alpha = 0.05f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = alimento.nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    maxLines   = 2
                )
                Text(
                    text     = "100g",
                    fontSize = 12.sp,
                    color    = Color.Gray
                )
                Spacer(Modifier.height(6.dp))

                // Macros con puntos de colores
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroDot(
                        color = Color(0xFFE53935),
                        text  = "P:${"%.1f".format(alimento.proteina)}g"
                    )
                    MacroDot(
                        color = Color(0xFFFFA726),
                        text  = "C:${"%.1f".format(alimento.carbo)}g"
                    )
                    MacroDot(
                        color = Color(0xFFFFD600),
                        text  = "G:${"%.1f".format(alimento.grasa)}g"
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "${"%.1f".format(alimento.kcal100g)} kcal",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = Color.Black
                )
                Spacer(Modifier.height(8.dp))
                IconButton(
                    onClick = if (enCarrito) onRemove else onAdd,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (enCarrito) Color(0xFFFFEBEE) else GreenPrimary.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = if (enCarrito) Icons.Default.Remove else Icons.Default.Add,
                        contentDescription = if (enCarrito) "Quitar" else "Agregar",
                        tint       = if (enCarrito) Color(0xFFE53935) else GreenPrimary,
                        modifier   = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Punto de color para macros ────────────────────────────────────────────────
@Composable
private fun MacroDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(3.dp))
        Text(text, fontSize = 11.sp, color = Color.DarkGray)
    }
}

// ── Bottom sheet del carrito ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartBottomSheet(
    cart: List<CartItem>,
    onRemove: (String) -> Unit,
    onClose: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Tu carrito",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${cart.size} items · ${"%.0f".format(cart.sumOf { it.kcal })} kcal total",
                fontSize = 13.sp,
                color    = Color.Gray
            )
            Spacer(Modifier.height(16.dp))

            // LazyColumn con key evita recomposiciones totales al modificar un solo ítem del carrito
            LazyColumn {
                items(
                    items = cart,
                    key   = { item -> item.alimento.fdcId }
                ) { item ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.alimento.nombre, fontWeight = FontWeight.Medium)
                            Text(
                                "${"%.0f".format(item.gramos)}g · ${"%.1f".format(item.kcal)} kcal",
                                fontSize = 12.sp,
                                color    = Color.Gray
                            )
                        }
                        IconButton(onClick = { onRemove(item.alimento.fdcId) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint               = Color(0xFFE53935)
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FoodListPreview() {
    NutriMetrixTheme {
        // Preview estática sin ViewModel
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Spacer(Modifier.height(52.dp))
            Text("Arma tu platillo", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Crea tu plato", fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(20.dp))
            AlimentoCard(
                alimento = Alimento("1","Pechuga de pollo", 165.0, 31.0, 0.0, 3.6),
                enCarrito = false,
                onAdd = {},
                onRemove = {}
            )
            Spacer(Modifier.height(10.dp))
            AlimentoCard(
                alimento = Alimento("2","Arroz blanco", 130.0, 2.7, 28.0, 0.3),
                enCarrito = true,
                onAdd = {},
                onRemove = {}
            )
        }
    }
}
