package com.example.nutrimetrix.data.remote.api

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Servicio Retrofit para subir imágenes a ImgBB y obtener una URL pública.
 * Se envía la imagen en Base64 y se recibe un JSON con la URL del hosting.
 */
interface ImgBBApiService {

    @FormUrlEncoded
    @POST("1/upload")
    suspend fun uploadImage(
        @Query("key")  apiKey:      String,
        @Field("image") base64Image: String
    ): ImgBBResponse
}

// ── Response DTOs ─────────────────────────────────────────────────────────────
data class ImgBBResponse(
    val data: ImgBBData
)

data class ImgBBData(
    val url: String   // URL pública de la imagen subida
)
