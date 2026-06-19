package com.example.nutrimetrix.core.di

import android.content.Context
import androidx.room.Room
import com.example.nutrimetrix.data.local.NutriMetrixDatabase
import com.example.nutrimetrix.data.local.dao.AlimentoDao
import com.example.nutrimetrix.data.local.dao.ComidaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): NutriMetrixDatabase {
        return Room.databaseBuilder(
            context,
            NutriMetrixDatabase::class.java,
            "nutrimetrix_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideAlimentoDao(db: NutriMetrixDatabase): AlimentoDao = db.alimentoDao()

    @Provides
    @Singleton
    fun provideComidaDao(db: NutriMetrixDatabase): ComidaDao = db.comidaDao()
}
