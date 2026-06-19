package com.example.nutrimetrix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.nutrimetrix.data.local.dao.AlimentoDao
import com.example.nutrimetrix.data.local.dao.ComidaDao
import com.example.nutrimetrix.data.local.entity.AlimentoEntity
import com.example.nutrimetrix.data.local.entity.ComidaEntity

@Database(
    entities = [AlimentoEntity::class, ComidaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NutriMetrixDatabase : RoomDatabase() {
    abstract fun alimentoDao(): AlimentoDao
    abstract fun comidaDao(): ComidaDao
}
