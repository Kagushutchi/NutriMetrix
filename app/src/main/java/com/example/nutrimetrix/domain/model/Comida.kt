package com.example.nutrimetrix.domain.model

import java.util.Date

/**
 * Modelo de dominio de una comida registrada por el usuario.
 */
data class Comida(
    val id:            String = "",
    val userId:        String,
    val nombre:        String,
    val tipo:          String, // DESAYUNO, ALMUERZO, MERIENDA, CENA, COLACIÓN
    val totalKcal:     Double,
    val proteinas:     Double,
    val carbohidratos: Double,
    val grasas:        Double,
    val timestamp:     Date,
    val fecha:         String, // Formato yyyy-MM-dd
    val url:           String = ""
)

/**
 * Representa un ingrediente detectado por la IA con su peso estimado.
 */
data class IngredienteDetectado(
    val nombre: String,
    val gramos: Double
)

/**
 * Resultado del análisis de imagen de comida procesado por IA y USDA.
 */
data class AnalisisComidaIa(
    val descripcion:   String,
    val ingredientes:  List<IngredienteDetectado>,
    val totalKcal:     Double,
    val proteinas:     Double,
    val carbohidratos: Double,
    val grasas:        Double
)
