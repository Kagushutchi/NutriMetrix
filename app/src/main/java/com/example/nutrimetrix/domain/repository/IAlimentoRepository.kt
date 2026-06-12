package com.example.nutrimetrix.domain.repository

import com.example.nutrimetrix.domain.model.Alimento
import com.example.nutrimetrix.domain.model.AnalisisComidaIa
import com.example.nutrimetrix.domain.model.Comida
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz del repositorio de alimentos de USDA y comidas registradas por el usuario.
 */
interface IAlimentoRepository {

    /**
     * Busca alimentos por coincidencia de texto en la base de datos de USDA.
     */
    suspend fun searchAlimentos(query: String): List<Alimento>

    /**
     * Guarda una comida para un usuario.
     */
    suspend fun saveComida(userId: String, comida: Comida): Result<Unit>

    /**
     * Retorna un flujo de la lista de comidas registradas de un usuario.
     */
    fun getComidas(userId: String): Flow<List<Comida>>

    /**
     * Retorna un flujo de la lista de comidas de hoy de un usuario.
     */
    fun getComidasDeHoy(userId: String): Flow<List<Comida>>

    /**
     * Obtiene el detalle de una comida específica.
     */
    suspend fun getComidaById(userId: String, comidaId: String): Comida?

    /**
     * Sube una imagen (codificada en Base64 o desde una URI) y obtiene su URL pública.
     */
    suspend fun uploadImage(imageUriStr: String): String

    /**
     * Analiza una imagen en Base64 utilizando IA (Gemini) y USDA para calcular ingredientes y macros.
     */
    suspend fun analyzeImage(base64Image: String): Result<AnalisisComidaIa>
}
