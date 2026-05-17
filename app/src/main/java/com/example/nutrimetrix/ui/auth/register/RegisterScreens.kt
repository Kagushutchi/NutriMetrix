package com.example.nutrimetrix.ui.auth.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutrimetrix.ui.auth.AuthUiState
import com.example.nutrimetrix.ui.auth.AuthViewModel
import com.example.nutrimetrix.ui.theme.GreenDark
import com.example.nutrimetrix.ui.theme.GreenLight
import com.example.nutrimetrix.ui.theme.GreenPrimary
import com.example.nutrimetrix.ui.theme.NutriMetrixTheme

// ══════════════════════════════════════════════════════════════════════════════
// PASO 2 — Datos personales
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun RegisterStep2Screen(
    onNext: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var peso   by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var edad   by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }

    val isValid = peso.isNotBlank() && altura.isNotBlank() &&
                  edad.isNotBlank() && genero.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Text("Datos personales", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(40.dp))

        NutriTextField(
            label         = "Peso (kg)",
            value         = peso,
            onValueChange = { peso = it }
        )
        Spacer(Modifier.height(16.dp))
        NutriTextField(
            label         = "Altura (cm)",
            value         = altura,
            onValueChange = { altura = it }
        )
        Spacer(Modifier.height(16.dp))
        NutriTextField(
            label         = "Edad (años)",
            value         = edad,
            onValueChange = { edad = it }
        )

        Text("Género", fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GenderButton(
                label    = "Masculino",
                icon     = Icons.Default.Person,
                selected = genero == "MASCULINO",
                onClick  = { genero = "MASCULINO" },
                modifier = Modifier.weight(1f)
            )
            GenderButton(
                label    = "Femenino",
                icon     = Icons.Default.Person,
                selected = genero == "FEMENINO",
                onClick  = { genero = "FEMENINO" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(32.dp))

        NutriButton(
            text    = "Siguiente",
            enabled = isValid,
            onClick = {
                viewModel.updateStep2(
                    peso   = peso.toDouble(),
                    altura = altura.toInt(),
                    edad   = edad.toInt(),
                    genero = genero
                )
                onNext()
            }
        )
        Spacer(Modifier.height(32.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PASO 3 — Objetivo
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun RegisterStep3Screen(
    onNext: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var objetivoSeleccionado by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Text("Objetivo", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))

        ObjetivoCard(
            title          = "Déficit",
            subtitle       = "Perder Peso",
            icon           = Icons.Default.TrendingDown,
            gradientColors = listOf(Color(0xFFFF8C00), Color(0xFFFF3B30)),
            selected       = objetivoSeleccionado == "DEFICIT",
            onClick        = { objetivoSeleccionado = "DEFICIT" }
        )
        Spacer(Modifier.height(16.dp))
        ObjetivoCard(
            title          = "Mantenimiento",
            subtitle       = "Mantener peso",
            icon           = Icons.Default.ShowChart,
            gradientColors = listOf(Color(0xFF007AFF), Color(0xFF0051FF)),
            selected       = objetivoSeleccionado == "MANTENIMIENTO",
            onClick        = { objetivoSeleccionado = "MANTENIMIENTO" }
        )
        Spacer(Modifier.height(16.dp))
        ObjetivoCard(
            title          = "Superávit",
            subtitle       = "Ganar Peso",
            icon           = Icons.Default.TrendingUp,
            gradientColors = listOf(Color(0xFF2FBF5B), Color(0xFF16592A)),
            selected       = objetivoSeleccionado == "SUPERAVIT",
            onClick        = { objetivoSeleccionado = "SUPERAVIT" }
        )

        Spacer(Modifier.weight(1f))

        NutriButton(
            text    = "Siguiente",
            enabled = objetivoSeleccionado.isNotEmpty(),
            onClick = {
                viewModel.updateStep3(objetivoSeleccionado)
                onNext()
            }
        )
        Spacer(Modifier.height(32.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PASO 4 — Actividad física y peso ideal
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun RegisterStep4Screen(
    onNext: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var pesoIdeal       by remember { mutableStateOf("") }
    var nivelSeleccionado by remember { mutableStateOf("") }

    // Leer el objetivo y peso actual del ViewModel para validar peso ideal
    val registerData by viewModel.registerData.collectAsStateWithLifecycle()
    val objetivo   = registerData.objetivo
    val pesoActual = registerData.peso

    val niveles = listOf(
        Triple("SEDENTARIO", "Sedentario",  "Poco o ningún ejercicio"),
        Triple("LIGERO",     "Ligero",       "Ejercicio 1-3 días/semana"),
        Triple("MODERADO",   "Moderado",     "Ejercicio 3-5 días/semana"),
        Triple("ACTIVO",     "Activo",       "Ejercicio 6-7 días/semana"),
        Triple("MUY_ACTIVO", "Muy activo",   "Ejercicio intenso diario")
    )

    // ── Validación de peso ideal según objetivo ───────────────────────────────
    val pesoIdealNum = pesoIdeal.toDoubleOrNull()

    val pesoIdealError: String? = when {
        objetivo == "MANTENIMIENTO" -> null  // no aplica, campo deshabilitado
        pesoIdealNum == null && pesoIdeal.isNotBlank() -> "Ingresá un número válido"
        objetivo == "DEFICIT" && pesoIdealNum != null && pesoIdealNum >= pesoActual ->
            "Para déficit el peso ideal debe ser menor a tu peso actual (${pesoActual}kg)"
        objetivo == "SUPERAVIT" && pesoIdealNum != null && pesoIdealNum <= pesoActual ->
            "Para superávit el peso ideal debe ser mayor a tu peso actual (${pesoActual}kg)"
        else -> null
    }

    val pesoIdealValido = when (objetivo) {
        "MANTENIMIENTO" -> true   // no requiere peso ideal
        else -> pesoIdealNum != null && pesoIdealError == null
    }

    val isValid = nivelSeleccionado.isNotEmpty() && pesoIdealValido

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Text("Actividad Física", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(40.dp))

        // Campo peso ideal — deshabilitado si es mantenimiento
        if (objetivo == "MANTENIMIENTO") {
            OutlinedTextField(
                value         = pesoActual.toString(),
                onValueChange = {},
                label         = { Text("Peso Ideal (kg)") },
                modifier      = Modifier.fillMaxWidth(),
                enabled       = false,
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color.LightGray,
                    disabledTextColor   = Color.Gray
                ),
                supportingText = {
                    Text("En mantenimiento tu peso ideal es tu peso actual",
                        fontSize = 11.sp, color = Color.Gray)
                }
            )
        } else {
            NutriTextField(
                label        = "Peso Ideal (kg)",
                value        = pesoIdeal,
                onValueChange = { pesoIdeal = it },
                isError      = pesoIdealError != null,
                errorMessage = pesoIdealError
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Nivel de actividad",
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(12.dp))

        niveles.forEach { (key, titulo, desc) ->
            ActivityCard(
                title       = titulo,
                description = desc,
                selected    = nivelSeleccionado == key,
                onClick     = { nivelSeleccionado = key }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        NutriButton(
            text    = "Calcular plan",
            enabled = isValid,
            onClick = {
                val pesoIdealFinal = if (objetivo == "MANTENIMIENTO") {
                    pesoActual
                } else {
                    pesoIdeal.toDouble()
                }
                viewModel.updateStep4(pesoIdealFinal, nivelSeleccionado)
                onNext()
            }
        )
        Spacer(Modifier.height(32.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// RESULTADO — Calorías y macros calculados
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun RegisterResultScreen(
    onFinish: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val result  by viewModel.nutritionResult.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Mostrar loading si todavía no hay resultado
    if (result == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenPrimary)
        }
        return
    }

    val r = result!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Text("Tu plan nutricional", fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(24.dp))

        // Card calorías con gradiente verde
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(GreenLight, GreenDark)))
                .padding(vertical = 32.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Calorías diarias",
                    color    = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
                Text(
                    text       = "${r.caloriasObjetivo}",
                    color      = Color.White,
                    fontSize   = 52.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("kcal", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Macronutrientes",
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MacroCard("%.1fg".format(r.proteinas),     "Prote",  Color(0xFFE53935), Modifier.weight(1f))
            MacroCard("%.1fg".format(r.carbohidratos), "CarboH", Color(0xFFFFA726), Modifier.weight(1f))
            MacroCard("%.1fg".format(r.grasas),        "Grasas", Color(0xFFFFD600), Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // Tiempo estimado
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            colors   = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🗓️  Tiempo estimado", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(r.semanasEstimadas, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Para alcanzar tu objetivo de manera efectiva",
                    fontSize = 12.sp,
                    color    = Color.Gray
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Error si hubo problema al guardar
        if (uiState is AuthUiState.Error) {
            Text(
                text     = (uiState as AuthUiState.Error).message,
                color    = Color.Red,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
        }

        NutriButton(
            text    = if (uiState is AuthUiState.Loading) "Guardando..." else "Siguiente",
            enabled = uiState !is AuthUiState.Loading,
            // guardarUsuarioEnFirestore llama onFinish directamente,
            // SIN pasar por AuthUiState.Success para evitar doble navegación
            onClick = { viewModel.guardarUsuarioEnFirestore(onFinish) }
        )
        Spacer(Modifier.height(32.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTES REUTILIZABLES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun NutriTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            label         = { Text(label) },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            isError       = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine    = true,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = GreenPrimary,
                unfocusedBorderColor = Color.LightGray,
                errorBorderColor     = Color.Red
            )
        )
        if (isError && errorMessage != null) {
            Text(
                text     = errorMessage,
                color    = Color.Red,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
fun NutriButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun GenderButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) GreenPrimary else Color.LightGray
    val bgColor     = if (selected) GreenPrimary.copy(alpha = 0.08f) else Color.White

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label,
            tint = if (selected) GreenPrimary else Color.Gray)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 14.sp,
            color = if (selected) GreenPrimary else Color.Gray)
    }
}

@Composable
private fun ObjetivoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .border(
                width  = if (selected) 3.dp else 0.dp,
                color  = Color.White.copy(alpha = if (selected) 0.8f else 0f),
                shape  = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = Color.White,
                modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title,    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ActivityCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor     = if (selected) GreenPrimary.copy(alpha = 0.1f) else Color(0xFFF5F5F5)
    val borderColor = if (selected) GreenPrimary else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Text(title,       fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(description, fontSize   = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun MacroCard(value: String, label: String, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Step3Preview() {
    NutriMetrixTheme {
        RegisterStep3Screen(onNext = {})
    }
}
