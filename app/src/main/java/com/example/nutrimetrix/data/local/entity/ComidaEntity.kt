package com.example.nutrimetrix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nutrimetrix.domain.model.Comida
import java.util.Date

@Entity(tableName = "comidas")
data class ComidaEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val nombre: String,
    val tipo: String,
    val totalKcal: Double,
    val proteinas: Double,
    val carbohidratos: Double,
    val grasas: Double,
    val timestamp: Long,
    val fecha: String,
    val url: String,
    val isSynced: Boolean = false
)

fun ComidaEntity.toDomain(): Comida {
    return Comida(
        id = id,
        userId = userId,
        nombre = nombre,
        tipo = tipo,
        totalKcal = totalKcal,
        proteinas = proteinas,
        carbohidratos = carbohidratos,
        grasas = grasas,
        timestamp = Date(timestamp),
        fecha = fecha,
        url = url
    )
}

fun Comida.toEntity(isSynced: Boolean = false): ComidaEntity {
    return ComidaEntity(
        id = id,
        userId = userId,
        nombre = nombre,
        tipo = tipo,
        totalKcal = totalKcal,
        proteinas = proteinas,
        carbohidratos = carbohidratos,
        grasas = grasas,
        timestamp = timestamp.time,
        fecha = fecha,
        url = url,
        isSynced = isSynced
    )
}
