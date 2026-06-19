package com.example.nutrimetrix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nutrimetrix.data.local.entity.AlimentoEntity

@Dao
interface AlimentoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlimentos(alimentos: List<AlimentoEntity>)

    @Query("SELECT * FROM alimentos_cache WHERE searchQuery = :query")
    suspend fun searchAlimentos(query: String): List<AlimentoEntity>

    @Query("DELETE FROM alimentos_cache")
    suspend fun clearCache()
}
