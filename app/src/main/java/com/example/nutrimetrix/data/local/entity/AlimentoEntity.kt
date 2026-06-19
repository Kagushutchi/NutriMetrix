package com.example.nutrimetrix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nutrimetrix.domain.model.Alimento

@Entity(tableName = "alimentos_cache")
data class AlimentoEntity(
    @PrimaryKey
    val fdcId: String,
    val nombre: String,
    val kcal100g: Double,
    val proteina: Double,
    val carbo: Double,
    val grasa: Double,
    val searchQuery: String
)

fun AlimentoEntity.toDomain(): Alimento {
    return Alimento(
        fdcId = fdcId,
        nombre = nombre,
        kcal100g = kcal100g,
        proteina = proteina,
        carbo = carbo,
        grasa = grasa
    )
}

fun Alimento.toEntity(searchQuery: String): AlimentoEntity {
    return AlimentoEntity(
        fdcId = fdcId,
        nombre = nombre,
        kcal100g = kcal100g,
        proteina = proteina,
        carbo = carbo,
        grasa = grasa,
        searchQuery = searchQuery
    )
}
