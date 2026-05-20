package com.example.nutrimetrix.data.mapper

import com.example.nutrimetrix.data.remote.dto.NutrientNames
import com.example.nutrimetrix.data.remote.dto.UsdaFoodDto
import com.example.nutrimetrix.domain.model.Alimento

/**
 * Convierte un UsdaFoodDto (capa de datos) en un Alimento (capa de dominio).
 * Los valores nutricionales de la API son por 100g.
 */
fun UsdaFoodDto.toDomain(): Alimento {
    fun nutrientValue(name: String) =
        nutrients.firstOrNull { it.nutrientName == name }?.value ?: 0.0

    return Alimento(
        fdcId    = fdcId.toString(),
        nombre   = description,
        kcal100g = nutrientValue(NutrientNames.ENERGIA),
        proteina = nutrientValue(NutrientNames.PROTEINA),
        carbo    = nutrientValue(NutrientNames.CARBOHIDRATO),
        grasa    = nutrientValue(NutrientNames.GRASA)
    )
}
