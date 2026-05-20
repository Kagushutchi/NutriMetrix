package com.example.nutrimetrix.data.remote.dto

import com.google.gson.annotations.SerializedName

//  Respuesta raíz
data class UsdaSearchResponse(
    @SerializedName("foods") val foods: List<UsdaFoodDto> = emptyList()
)

// Un alimento en el resultado
data class UsdaFoodDto(
    @SerializedName("fdcId")         val fdcId:       Int    = 0,
    @SerializedName("description")   val description: String = "",
    @SerializedName("foodNutrients") val nutrients:   List<UsdaNutrientDto> = emptyList()
)

// Un nutriente del alimento
data class UsdaNutrientDto(
    @SerializedName("nutrientName") val nutrientName: String = "",
    @SerializedName("unitName")     val unitName:     String = "",
    @SerializedName("value")        val value:        Double = 0.0
)

//IDs de nutrientes que me sirven
object NutrientNames {
    const val ENERGIA    = "Energy"
    const val PROTEINA   = "Protein"
    const val GRASA      = "Total lipid (fat)"
    const val CARBOHIDRATO = "Carbohydrate, by difference"
}
