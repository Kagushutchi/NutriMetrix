package com.example.nutrimetrix.ui.food.camera

import android.Manifest
import android.net.Uri
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.nutrimetrix.data.remote.dto.IngredienteDetectado
import com.example.nutrimetrix.ui.theme.GreenDark
import com.example.nutrimetrix.ui.theme.GreenLight
import com.example.nutrimetrix.ui.theme.GreenPrimary

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

val tiposDeComidaCamara = listOf("Colación", "Desayuno", "Almuerzo", "Merienda", "Cena")

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()
    val tipoComida by viewModel.tipoComida.collectAsStateWithLifecycle()

    // Cuando se guarda exitosamente, volver al Home
    LaunchedEffect(uiState) {
        if (uiState is CameraUiState.Saved) {
            viewModel.resetState()
            onNavigateBack()
        }
    }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    when (uiState) {

        // ── Preview de CameraX ────────────────────────────────────────────────
        is CameraUiState.Idle -> {
            if (cameraPermission.status.isGranted) {
                CameraPreviewScreen(
                    imageCapture = viewModel.imageCapture,
                    onCapture    = { viewModel.capturarFoto() },
                    onClose      = onNavigateBack
                )
            } else {
                PermissionScreen(
                    onRequest = { cameraPermission.launchPermissionRequest() },
                    onBack    = onNavigateBack
                )
            }
        }

        // ── Gemini analizando ─────────────────────────────────────────────────
        is CameraUiState.Analyzing -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("Analizando tu plato...", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text("Gemini AI está procesando la imagen", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }

        // ── Resultado del análisis ────────────────────────────────────────────
        is CameraUiState.Result -> {
            val r = uiState as CameraUiState.Result
            ResultScreen(
                imageUri     = r.imageUri,
                descripcion  = r.descripcion,
                ingredientes = r.ingredientes,
                totalKcal    = r.totalKcal,
                proteinas    = r.proteinas,
                carbos       = r.carbos,
                grasas       = r.grasas,
                tipoComida   = tipoComida,
                onTipoChange = { viewModel.onTipoComidaChange(it) },
                onConfirm    = { viewModel.guardarRegistro(onNavigateBack) },
                onRetry      = { viewModel.resetState() },
                onClose      = { viewModel.resetState(); onNavigateBack() }
            )
        }

        // ── Guardando ─────────────────────────────────────────────────────────
        is CameraUiState.Saving -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Guardando registro...", fontSize = 16.sp, color = Color.Gray)
                }
            }
        }

        // ── Error ─────────────────────────────────────────────────────────────
        is CameraUiState.Error -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text("❌", fontSize = 44.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        (uiState as CameraUiState.Error).message,
                        color     = Color.Red,
                        fontSize  = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.resetState() },
                        colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) { Text("Reintentar") }
                }
            }
        }

        CameraUiState.Saved -> { /* handled by LaunchedEffect */ }
    }
}

// ── Preview de CameraX con AndroidView ───────────────────────────────────────
@Composable
private fun CameraPreviewScreen(
    imageCapture: androidx.camera.core.ImageCapture,
    onCapture:    () -> Unit,
    onClose:      () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // AndroidView para incrustar el PreviewView de CameraX en Compose
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update   = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = CameraPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture   // bind del use case del ViewModel
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Botón cerrar
        IconButton(
            onClick  = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
        }

        // Botón capturar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            // Anillo exterior
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                // Botón interior blanco
                Button(
                    onClick  = onCapture,
                    modifier = Modifier.size(64.dp),
                    shape    = CircleShape,
                    colors   = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(0.dp)
                ) {}
            }
        }
    }
}

// ── Pantalla de permiso denegado ──────────────────────────────────────────────
@Composable
private fun PermissionScreen(onRequest: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("📷", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("Permiso de cámara requerido", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "NutriMetrix necesita acceso a la cámara para analizar tus platos",
                fontSize  = 14.sp,
                color     = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onRequest,
                modifier= Modifier.fillMaxWidth().height(52.dp),
                shape   = RoundedCornerShape(14.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) { Text("Dar permiso") }
        }
        IconButton(
            onClick  = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
        }
    }
}

// ── Pantalla de resultado del análisis ───────────────────────────────────────
@Composable
private fun ResultScreen(
    imageUri:     Uri,
    descripcion:  String,
    ingredientes: List<IngredienteDetectado>,
    totalKcal:    Double,
    proteinas:    Double,
    carbos:       Double,
    grasas:       Double,
    tipoComida:   String,
    onTipoChange: (String) -> Unit,
    onConfirm:    () -> Unit,
    onRetry:      () -> Unit,
    onClose:      () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(52.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Análisis", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Foto capturada
        Image(
            painter            = rememberAsyncImagePainter(imageUri),
            contentDescription = "Foto del plato",
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(Modifier.height(16.dp))

        Text(descripcion, fontSize = 15.sp, fontWeight = FontWeight.Medium)

        Spacer(Modifier.height(16.dp))

        // Card calorías
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(GreenLight, GreenDark)))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Calorias diarias", color = Color.White.copy(0.85f), fontSize = 13.sp)
                Text("${"%.0f".format(totalKcal)}", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text("kcal", color = Color.White.copy(0.85f), fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Macros
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MacroBox("${"%.1f".format(proteinas)}g", "Prote",  Color(0xFFE53935), Modifier.weight(1f))
            MacroBox("${"%.1f".format(carbos)}g",    "CarboH", Color(0xFFFFA726), Modifier.weight(1f))
            MacroBox("${"%.1f".format(grasas)}g",    "Grasas", Color(0xFFFFD600), Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // Ingredientes
        Text("Ingredientes detectados", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ingredientes.forEach { ing ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(GreenPrimary))
                        Spacer(Modifier.width(10.dp))
                        Text("${ing.gramos.toInt()}g de ${ing.nombre}", fontSize = 14.sp)
                    }
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Dropdown tipo de comida
        TipoComidaDropdown(selected = tipoComida, onSelect = onTipoChange)

        Spacer(Modifier.height(20.dp))

        // Botón guardar
        Button(
            onClick  = onConfirm,
            enabled  = tipoComida.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Añadir al registro", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick  = onRetry,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Text("Tomar otra foto", color = GreenPrimary)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MacroBox(value: String, label: String, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(color).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(0.85f), fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TipoComidaDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value         = selected.ifEmpty { "Seleccionar tipo de comida" },
            onValueChange = {},
            readOnly      = true,
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier      = Modifier.menuAnchor().fillMaxWidth(),
            shape         = RoundedCornerShape(14.dp),
            isError       = selected.isEmpty(),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = GreenPrimary,
                unfocusedBorderColor = if (selected.isEmpty()) Color.Red.copy(0.5f) else Color.LightGray
            ),
            supportingText = if (selected.isEmpty()) {
                { Text("Requerido para guardar", fontSize = 11.sp, color = Color.Red) }
            } else null
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tiposDeComidaCamara.forEach { tipo ->
                DropdownMenuItem(
                    text    = { Text(tipo) },
                    onClick = { onSelect(tipo.uppercase()); expanded = false }
                )
            }
        }
    }
}
