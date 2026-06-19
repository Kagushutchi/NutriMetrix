package com.example.nutrimetrix.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.nutrimetrix.BuildConfig
import com.example.nutrimetrix.data.mapper.toDomain
import com.example.nutrimetrix.data.remote.api.GeminiApiService
import com.example.nutrimetrix.data.remote.api.GeminiInlineData
import com.example.nutrimetrix.data.remote.api.GeminiRequest
import com.example.nutrimetrix.data.remote.api.GeminiRequestContent
import com.example.nutrimetrix.data.remote.api.GeminiRequestPart
import com.example.nutrimetrix.data.remote.api.ImgBBApiService
import com.example.nutrimetrix.data.remote.api.UsdaApiService
import com.example.nutrimetrix.data.remote.dto.GeminiAnalisisResult
import com.example.nutrimetrix.data.remote.dto.IngredienteDetectado as DtoIngredienteDetectado
import com.example.nutrimetrix.domain.model.Alimento
import com.example.nutrimetrix.domain.model.AnalisisComidaIa
import com.example.nutrimetrix.domain.model.Comida
import com.example.nutrimetrix.domain.model.IngredienteDetectado as DomainIngredienteDetectado
import com.example.nutrimetrix.data.local.dao.AlimentoDao
import com.example.nutrimetrix.data.local.dao.ComidaDao
import com.example.nutrimetrix.data.local.entity.toDomain
import com.example.nutrimetrix.data.local.entity.toEntity
import com.example.nutrimetrix.domain.repository.IAlimentoRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlimentoRepositoryImpl @Inject constructor(
    private val usdaApi:      UsdaApiService,
    private val geminiApi:    GeminiApiService,
    private val imgBBApi:     ImgBBApiService,
    private val firestore:    FirebaseFirestore,
    private val alimentoDao:  AlimentoDao,
    private val comidaDao:    ComidaDao,
    @ApplicationContext private val context: Context
) : IAlimentoRepository {

    override suspend fun searchAlimentos(query: String): List<Alimento> {
        val trimmedQuery = query.trim()
        val cached = alimentoDao.searchAlimentos(trimmedQuery)
        if (cached.isNotEmpty()) {
            return cached.map { it.toDomain() }
        }
        return try {
            val response = usdaApi.searchFoods(
                apiKey = BuildConfig.USDA_API_KEY,
                query  = query
            )
            val domainList = response.foods.map { it.toDomain() }
            val entities = domainList.map { it.toEntity(trimmedQuery) }
            alimentoDao.insertAlimentos(entities)
            domainList
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveComida(userId: String, comida: Comida): Result<Unit> {
        return try {
            val docRef = if (comida.id.isNotEmpty()) {
                firestore.collection("usuarios").document(userId).collection("comidas").document(comida.id)
            } else {
                firestore.collection("usuarios").document(userId).collection("comidas").document()
            }
            val finalId = docRef.id
            val finalComida = comida.copy(id = finalId)
            
            var isSynced = false
            try {
                val comidaMap = mapOf(
                    "userId"        to finalComida.userId,
                    "nombre"        to finalComida.nombre,
                    "tipo"          to finalComida.tipo,
                    "totalKcal"     to finalComida.totalKcal,
                    "proteinas"     to finalComida.proteinas,
                    "carbohidratos" to finalComida.carbohidratos,
                    "grasas"        to finalComida.grasas,
                    "timestamp"     to Timestamp(finalComida.timestamp),
                    "fecha"         to finalComida.fecha,
                    "url"           to finalComida.url
                )
                docRef.set(comidaMap).await()
                isSynced = true
            } catch (e: Exception) {
                Log.e("AlimentoRepository", "Error syncing to Firestore, saving offline", e)
            }

            comidaDao.insertComida(finalComida.toEntity(isSynced = isSynced))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getComidas(userId: String): Flow<List<Comida>> = channelFlow {
        val roomJob = launch {
            comidaDao.getComidas(userId).collect { list ->
                send(list.map { it.toDomain() })
            }
        }

        val firestoreListener = firestore.collection("usuarios").document(userId)
            .collection("comidas")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AlimentoRepository", "Error listening to Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val entities = snapshot.documents.mapNotNull { doc ->
                        val comida = mapDocumentToComida(doc)
                        comida?.toEntity(isSynced = true)
                    }
                    if (entities.isNotEmpty()) {
                        launch {
                            try {
                                comidaDao.insertComidas(entities)
                            } catch (e: Exception) {
                                Log.e("AlimentoRepository", "Error inserting remote meals into Room", e)
                            }
                        }
                    }
                }
            }

        awaitClose {
            roomJob.cancel()
            firestoreListener.remove()
        }
    }

    override fun getComidasDeHoy(userId: String): Flow<List<Comida>> = channelFlow {
        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(Date())
            
        val roomJob = launch {
            comidaDao.getComidasDeHoy(userId, hoy).collect { list ->
                send(list.map { it.toDomain() })
            }
        }

        val firestoreListener = firestore.collection("usuarios").document(userId)
            .collection("comidas")
            .whereGreaterThanOrEqualTo("fecha", hoy)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AlimentoRepository", "Error listening to Firestore today", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val entities = snapshot.documents.mapNotNull { doc ->
                        val comida = mapDocumentToComida(doc)
                        comida?.toEntity(isSynced = true)
                    }
                    if (entities.isNotEmpty()) {
                        launch {
                            try {
                                comidaDao.insertComidas(entities)
                            } catch (e: Exception) {
                                Log.e("AlimentoRepository", "Error inserting remote meals today into Room", e)
                            }
                        }
                    }
                }
            }

        awaitClose {
            roomJob.cancel()
            firestoreListener.remove()
        }
    }

    override suspend fun getComidaById(userId: String, comidaId: String): Comida? {
        return try {
            val localComida = comidaDao.getComidaById(userId, comidaId)
            if (localComida != null) {
                localComida.toDomain()
            } else {
                val doc = firestore.collection("usuarios").document(userId)
                    .collection("comidas").document(comidaId).get().await()
                if (doc.exists()) {
                    val comida = mapDocumentToComida(doc)
                    if (comida != null) {
                        comidaDao.insertComida(comida.toEntity(isSynced = true))
                    }
                    comida
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun uploadImage(imageUriStr: String): String {
        return try {
            val uri = Uri.parse(imageUriStr)
            val base64 = uriToBase64(uri) ?: return ""
            val response = imgBBApi.uploadImage(
                apiKey      = BuildConfig.IMGBB_API_KEY,
                base64Image = base64
            )
            response.data.url
        } catch (e: Exception) {
            Log.e("AlimentoRepository", "Error subiendo imagen a ImgBB", e)
            ""
        }
    }

    override suspend fun analyzeImage(base64Image: String): Result<AnalisisComidaIa> {
        return try {
            val prompt = """
                Actúa como un nutricionista experto. Analiza detalladamente esta imagen de comida para estimar los ingredientes y el tamaño de las porciones (en gramos) de la forma más precisa posible, basándose en las proporciones visuales de un plato estándar.
                
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
                                        data     = base64Image
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

            var totalKcal  = 0.0
            var totalProt  = 0.0
            var totalCarbs = 0.0
            var totalFat   = 0.0

            val domainIngredientes = analisis.ingredientes.map {
                DomainIngredienteDetectado(it.nombre, it.gramos)
            }

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

            Result.success(
                AnalisisComidaIa(
                    descripcion   = analisis.descripcion,
                    ingredientes  = domainIngredientes,
                    totalKcal     = totalKcal,
                    proteinas     = totalProt,
                    carbohidratos = totalCarbs,
                    grasas        = totalFat
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapDocumentToComida(doc: com.google.firebase.firestore.DocumentSnapshot): Comida? {
        val timestamp = doc.getTimestamp("timestamp") ?: return null
        return Comida(
            id            = doc.id,
            userId        = doc.getString("userId") ?: "",
            nombre        = doc.getString("nombre") ?: "",
            tipo          = doc.getString("tipo") ?: "",
            totalKcal     = doc.getDouble("totalKcal") ?: 0.0,
            proteinas     = doc.getDouble("proteinas") ?: 0.0,
            carbohidratos = doc.getDouble("carbohidratos") ?: 0.0,
            grasas        = doc.getDouble("grasas") ?: 0.0,
            timestamp     = timestamp.toDate(),
            fecha         = doc.getString("fecha") ?: "",
            url           = doc.getString("url") ?: ""
        )
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val resolver = context.contentResolver
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val maxSize = 800
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
            options.inJustDecodeBounds = false
            val reducedBitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            val out = ByteArrayOutputStream()
            reducedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            Base64.getEncoder().encodeToString(out.toByteArray())
        } catch (e: Exception) {
            Log.e("AlimentoRepository", "Error procesando imagen para Base64", e)
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

    private fun parseGeminiJson(rawText: String): GeminiAnalisisResult {
        val json = rawText.replace("```json", "").replace("```", "").trim()
        return try {
            @Suppress("UNCHECKED_CAST")
            val map = Gson().fromJson(json, Map::class.java) as Map<String, Any>
            val descripcion = map["descripcion"] as? String ?: "Plato detectado"
            @Suppress("UNCHECKED_CAST")
            val ings = (map["ingredientes"] as? List<Map<String, Any>> ?: emptyList()).map {
                DtoIngredienteDetectado(
                    nombre = it["nombre"] as? String ?: "",
                    gramos = (it["gramos"] as? Double) ?: (it["gramos"] as? Long)?.toDouble() ?: 100.0
                )
            }
            @Suppress("UNCHECKED_CAST")
            val busquedas = map["busquedas_usda"] as? List<String> ?: emptyList()
            GeminiAnalisisResult(descripcion, ings, busquedas)
        } catch (e: Exception) {
            GeminiAnalisisResult(
                descripcion   = "Plato detectado",
                ingredientes  = listOf(DtoIngredienteDetectado("Ingrediente", 100.0)),
                busquedasUsda = listOf("food")
            )
        }
    }
}
