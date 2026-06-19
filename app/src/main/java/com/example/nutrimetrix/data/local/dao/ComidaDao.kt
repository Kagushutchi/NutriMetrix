package com.example.nutrimetrix.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nutrimetrix.data.local.entity.ComidaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComidaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComida(comida: ComidaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComidas(comidas: List<ComidaEntity>)

    @Query("SELECT * FROM comidas WHERE userId = :userId ORDER BY timestamp DESC")
    fun getComidas(userId: String): Flow<List<ComidaEntity>>

    @Query("SELECT * FROM comidas WHERE userId = :userId AND fecha >= :hoy ORDER BY timestamp DESC")
    fun getComidasDeHoy(userId: String, hoy: String): Flow<List<ComidaEntity>>

    @Query("SELECT * FROM comidas WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedComidas(userId: String): List<ComidaEntity>

    @Query("SELECT * FROM comidas WHERE userId = :userId AND id = :comidaId LIMIT 1")
    suspend fun getComidaById(userId: String, comidaId: String): ComidaEntity?

    @Delete
    suspend fun deleteComida(comida: ComidaEntity)

    @Query("DELETE FROM comidas")
    suspend fun clearComidas()
}
