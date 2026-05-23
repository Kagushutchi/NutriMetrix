package com.example.nutrimetrix.data.remote.api

import com.example.nutrimetrix.data.remote.dto.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {


    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun analyzeImage(
        @Query("key")  apiKey:  String,
        @Body          request: GeminiRequest
    ): GeminiResponse
}

// ── Request body para Gemini ──────────────────────────────────────────────────
data class GeminiRequest(
    val contents:         List<GeminiRequestContent>,
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

data class GeminiRequestContent(
    val parts: List<GeminiRequestPart>
)

data class GeminiRequestPart(
    val text:         String?              = null,
    val inlineData:   GeminiInlineData?    = null
)

data class GeminiInlineData(
    val mimeType: String,   // "image/jpeg"
    val data:     String    // base64 de la imagen
)

data class GeminiGenerationConfig(
    val temperature:     Double = 0.2,
    val maxOutputTokens: Int    = 1024
)
