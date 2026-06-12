package com.example.nutrimetrix.ui.food.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrimetrix.BuildConfig
import com.example.nutrimetrix.data.mapper.toDomain
import com.example.nutrimetrix.data.remote.api.GeminiApiService
import com.example.nutrimetrix.data.remote.api.GeminiInlineData
import com.example.nutrimetrix.data.remote.api.GeminiRequest
import com.example.nutrimetrix.data.remote.api.GeminiRequestContent
import com.example.nutrimetrix.data.remote.api.GeminiRequestPart
import com.example.nutrimetrix.data.remote.api.ImgBBApiService
import com.example.nutrimetrix.data.remote.api.UsdaApiService
import com.example.nutrimetrix.data.remote.dto.IngredienteDetectado
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.inject.Inject
import androidx.core.graphics.scale
import android.util.Log

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
    private val geminiApi:    GeminiApiService,
    private val usdaApi:      UsdaApiService,
    private val imgBBApi:     ImgBBApiService,
    private val firebaseAuth: FirebaseAuth,
    private val firestore:    FirebaseFirestore,
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

                val prompt = """
    Actúa como un nutricionista experto. Analiza detalladamente esta imagen de comida para estimar los ingredientes y el tamaño de las porciones (en gramos) de la forma más precisa posible, basándote en las proporciones visuales de un plato estándar.
    
    Reglas estrictas:
    1. Desglosa los platos complejos en sus ingredientes básicos (ej: en lugar de "tarta de jamón", usa "masa de tarta", "jamón cocido", "queso mozzarella").
    2. Responde ÚNICAMENTE con un objeto JSON válido. No uses bloques de código (```json), ni Markdown, ni ningún texto adicional introductorio o de despedida.
    3. Asegúrate de que 'busquedas_usda' tenga exactamente la misma cantidad de elementos y en el mismo orden que 'ingredientes'. Utiliza nombres crudos y precisos en inglés para la base de datos USDA (ej: "raw chicken breast", "cooked white rice").
    
    El JSON debe tener EXACTAMENTE esta estructura:
    {
      "descripcion": "Nombre del plato completo",
      "ingredientes": [
        { "nombre": "Nombre del ingrediente en español", "gramos": 150 }
      ],
      "busquedas_usda": ["exact ingredient name in english"]
    }
""".trimIndent()

                val geminiResponse = geminiApi.analyzeImage(
                    apiKey  = BuildConfig.GEMINI_API_KEY,
                    request = GeminiRequest(
                        contents = listOf(
                            GeminiRequestContent(
                                parts = listOf(
                                    GeminiRequestPart(
                                        inlineData = GeminiInlineData(
                                            mimeType = "image/jpeg",
                                            data     = base64
                                        )
                                    ),
                                    GeminiRequestPart(text = prompt)
                                )
                            )
                        )
                    )
                )

                val rawText = geminiResponse.candidates
                    .firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("Gemini no devolvió respuesta")

                val analisis = parseGeminiJson(rawText)

                // Buscar cada ingrediente en USDA
                var totalKcal  = 0.0
                var totalProt  = 0.0
                var totalCarbs = 0.0
                var totalFat   = 0.0

                analisis.busquedasUsda.forEachIndexed { index, query ->
                    try {
                        val result = usdaApi.searchFoods(
                            apiKey   = BuildConfig.USDA_API_KEY,
                            query    = query,
                            pageSize = 1
                        )
                        val alimento = result.foods.firstOrNull()?.toDomain()
                        val gramos   = analisis.ingredientes.getOrNull(index)?.gramos ?: 100.0
                        if (alimento != null) {
                            totalKcal  += alimento.kcal100g * gramos / 100
                            totalProt  += alimento.proteina  * gramos / 100
                            totalCarbs += alimento.carbo     * gramos / 100
                            totalFat   += alimento.grasa     * gramos / 100
                        }
                    } catch (_: Exception) { }
                }

                _uiState.value = CameraUiState.Result(
                    imageUri     = imageUri,
                    descripcion  = analisis.descripcion,
                    ingredientes = analisis.ingredientes,
                    totalKcal    = totalKcal,
                    proteinas    = totalProt,
                    carbos       = totalCarbs,
                    grasas       = totalFat
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
                val uid = firebaseAuth.currentUser?.uid ?: throw Exception("Sin usuario")

                // Subir imagen a ImgBB y obtener URL pública
                val imageUrl = subirImagenImgBB(estado.imageUri)

                val comidaMap = mapOf(
                    "userId"        to uid,
                    "nombre"        to estado.descripcion,
                    "tipo"          to _tipoComida.value,
                    "totalKcal"     to estado.totalKcal,
                    "proteinas"     to estado.proteinas,
                    "carbohidratos" to estado.carbos,
                    "grasas"        to estado.grasas,
                    "timestamp"     to com.google.firebase.Timestamp.now(),
                    "fecha"         to java.text.SimpleDateFormat(
                        "yyyy-MM-dd", java.util.Locale.getDefault()
                    ).format(java.util.Date()),
                    "url"           to imageUrl
                )

                firestore
                    .collection("usuarios").document(uid)
                    .collection("comidas")
                    .add(comidaMap).await()

                _uiState.value = CameraUiState.Saved
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = CameraUiState.Error(e.message ?: "Error al guardar")
            }
        }
    }

    // ── Subir imagen a ImgBB ──────────────────────────────────────────────────
    private suspend fun subirImagenImgBB(uri: Uri): String {
        return try {
            val base64 = uriToBase64(uri) ?: return ""
            val response = imgBBApi.uploadImage(
                apiKey      = BuildConfig.IMGBB_API_KEY,
                base64Image = base64
            )
            response.data.url
        } catch (e: Exception) {
            android.util.Log.e("ImgBB_Error", "Fallo al subir a ImgBB", e)
            ""
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────


// ...

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

            // 3. Ahora sí, cargar la imagen ya reducida en la memoria RAM
            options.inJustDecodeBounds = false
            val reducedBitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            // 4. Comprimir a JPEG (calidad 70) y convertir a Base64
            val out = ByteArrayOutputStream()
            reducedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)

            val imageBytes = out.toByteArray()
            // Opcional: Imprimir el peso en KB para confirmar que es pequeña
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

            // Calcula el mayor valor inSampleSize que sea potencia de 2 y
            // mantenga el ancho y alto mayores al requerido
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    private fun parseGeminiJson(rawText: String): com.example.nutrimetrix.data.remote.dto.GeminiAnalisisResult {
        val json = rawText.replace("```json", "").replace("```", "").trim()
        return try {
            @Suppress("UNCHECKED_CAST")
            val map = Gson().fromJson(json, Map::class.java) as Map<String, Any>

            val descripcion = map["descripcion"] as? String ?: "Plato detectado"

            @Suppress("UNCHECKED_CAST")
            val ings = (map["ingredientes"] as? List<Map<String, Any>> ?: emptyList()).map {
                IngredienteDetectado(
                    nombre = it["nombre"] as? String ?: "",
                    gramos = (it["gramos"] as? Double) ?: (it["gramos"] as? Long)?.toDouble() ?: 100.0
                )
            }

            @Suppress("UNCHECKED_CAST")
            val busquedas = map["busquedas_usda"] as? List<String> ?: emptyList()

            com.example.nutrimetrix.data.remote.dto.GeminiAnalisisResult(descripcion, ings, busquedas)
        } catch (e: Exception) {
            com.example.nutrimetrix.data.remote.dto.GeminiAnalisisResult(
                descripcion   = "Plato detectado",
                ingredientes  = listOf(IngredienteDetectado("Ingrediente", 100.0)),
                busquedasUsda = listOf("food")
            )
        }
    }

    fun resetState() { _uiState.value = CameraUiState.Idle }
}
