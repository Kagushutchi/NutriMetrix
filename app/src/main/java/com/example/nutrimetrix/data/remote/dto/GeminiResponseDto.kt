package com.example.nutrimetrix.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── Respuesta de Gemini API ───────────────────────────────────────────────────
data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate> = emptyList()
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContent
)

data class GeminiContent(
    @SerializedName("parts") val parts: List<GeminiPart> = emptyList()
)

data class GeminiPart(
    @SerializedName("text") val text: String = ""
)

// ── Modelo parseado del JSON que devuelve Gemini ──────────────────────────────
// Gemini va a responder con este formato JSON en su texto:
// {
//   "descripcion": "Pollo a la plancha con arroz blanco y ensalada",
//   "ingredientes": [
//     { "nombre": "Pechuga de pollo", "gramos": 150 },
//     { "nombre": "Arroz blanco",     "gramos": 100 }
//   ],
//   "busquedas_usda": ["chicken breast", "white rice", "mixed salad"]
// }
data class GeminiAnalisisResult(
    val descripcion:    String,
    val ingredientes:   List<IngredienteDetectado>,
    val busquedasUsda:  List<String>
)

data class IngredienteDetectado(
    val nombre: String,
    val gramos: Double
)
