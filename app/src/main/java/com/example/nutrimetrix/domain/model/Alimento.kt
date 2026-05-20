package com.example.nutrimetrix.domain.model

/**
 * Modelo de dominio de un alimento.
 * Todos los valores nutricionales son por 100g.
 */
data class Alimento(
    val fdcId:    String,
    val nombre:   String,
    val kcal100g: Double,
    val proteina: Double,
    val carbo:    Double,
    val grasa:    Double
)
