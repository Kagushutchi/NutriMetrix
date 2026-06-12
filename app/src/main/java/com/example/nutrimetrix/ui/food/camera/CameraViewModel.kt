package com.example.nutrimetrix.ui.food.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrimetrix.domain.model.Comida
import com.example.nutrimetrix.domain.model.IngredienteDetectado
import com.example.nutrimetrix.domain.repository.IAlimentoRepository
import com.example.nutrimetrix.domain.repository.IAuthRepository
import com.example.nutrimetrix.domain.usecase.AnalyzeImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.inject.Inject

// ── Estados ───────────────────────────────────────────────────────────────────
sealed class CameraUiState {
    object Idle      : CameraUiState()  // preview de CameraX activo
    object Analyzing : CameraUiState()  // Gemini procesando
    data class Result(
        val imageUri:    Uri,
        val descripcion: String,
        val ingredientes:List<IngredienteDetectado>,
        val totalKcal:   Double,
        val proteinas:   Double,
        val carbos:      Double,
        val grasas:      Double
    ) : CameraUiState()
    object Saving    : CameraUiState()
    object Saved     : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val analyzeImageUseCase: AnalyzeImageUseCase,
    private val alimentoRepository:    IAlimentoRepository,
    private val authRepository:        IAuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Idle)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _tipoComida = MutableStateFlow("")
    val tipoComida: StateFlow<String> = _tipoComida.asStateFlow()

    // ImageCapture use case de CameraX — la Screen lo bindea al lifecycle
    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    fun onTipoComidaChange(tipo: String) { _tipoComida.value = tipo }

    // ── Capturar foto con CameraX ─────────────────────────────────────────────
    fun capturarFoto() {
        val photoFile = File.createTempFile("nutrimetrix_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    analizarImagen(uri)
                }
                override fun onError(exc: ImageCaptureException) {
                    _uiState.value = CameraUiState.Error("Error al capturar: ${exc.message}")
                }
            }
        )
    }

    // ── Analizar con Gemini + buscar en USDA ──────────────────────────────────
    private fun analizarImagen(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Analyzing
            try {
                val base64 = uriToBase64(imageUri)
                    ?: throw Exception("No se pudo leer la imagen")

                val res = analyzeImageUseCase(base64).getOrThrow()

                _uiState.value = CameraUiState.Result(
                    imageUri     = imageUri,
                    descripcion  = res.descripcion,
                    ingredientes = res.ingredientes,
                    totalKcal    = res.totalKcal,
                    proteinas    = res.proteinas,
                    carbos       = res.carbohidratos,
                    grasas       = res.grasas
                )
            } catch (e: Exception) {
                _uiState.value = CameraUiState.Error(e.message ?: "Error al analizar")
            }
        }
    }

    // ── Guardar en Firestore + ImgBB ──────────────────────────────────────────
    fun guardarRegistro(onSuccess: () -> Unit) {
        val estado = _uiState.value as? CameraUiState.Result ?: return
        if (_tipoComida.value.isBlank()) return

        viewModelScope.launch {
            _uiState.value = CameraUiState.Saving
            try {
                val uid = authRepository.getCurrentUserId() ?: throw Exception("Sin usuario")

                // Subir imagen a ImgBB y obtener URL pública
                val imageUrl = alimentoRepository.uploadImage(estado.imageUri.toString())

                val comida = Comida(
                    id            = "",
                    userId        = uid,
                    nombre        = estado.descripcion,
                    tipo          = _tipoComida.value,
                    totalKcal     = estado.totalKcal,
                    proteinas     = estado.proteinas,
                    carbohidratos = estado.carbos,
                    grasas        = estado.grasas,
                    timestamp     = java.util.Date(),
                    fecha         = java.text.SimpleDateFormat(
                        "yyyy-MM-dd", java.util.Locale.getDefault()
                    ).format(java.util.Date()),
                    url           = imageUrl
                )

                alimentoRepository.saveComida(uid, comida).getOrThrow()

                _uiState.value = CameraUiState.Saved
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = CameraUiState.Error(e.message ?: "Error al guardar")
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val resolver = context.contentResolver

            // 1. Leer solo las dimensiones de la imagen (NO la carga en memoria)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            // 2. Calcular el factor de reducción (achica en potencias de 2)
            val maxSize = 800
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)

            // 3. Cargar la imagen ya reducida en la memoria RAM
            options.inJustDecodeBounds = false
            val reducedBitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            // 4. Comprimir a JPEG (calidad 70) y convertir a Base64
            val out = ByteArrayOutputStream()
            reducedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)

            val imageBytes = out.toByteArray()
            Log.d("ImageSize", "Tamaño final de la imagen: ${imageBytes.size / 1024} KB")

            Base64.getEncoder().encodeToString(imageBytes)

        } catch (e: Exception) {
            Log.e("CameraViewModel", "Error procesando imagen para Base64", e)
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun resetState() { _uiState.value = CameraUiState.Idle }
}
