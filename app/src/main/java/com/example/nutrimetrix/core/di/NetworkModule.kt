package com.example.nutrimetrix.core.di

import com.example.nutrimetrix.data.remote.api.GeminiApiService
import com.example.nutrimetrix.data.remote.api.ImgBBApiService
import com.example.nutrimetrix.data.remote.api.UsdaApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // OkHttp compartido por ambos clientes con timeout más largo para Gemini
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    // ── USDA ──────────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("usda")
    fun provideUsdaRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.nal.usda.gov/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideUsdaApiService(@Named("usda") retrofit: Retrofit): UsdaApiService =
        retrofit.create(UsdaApiService::class.java)

    // ── Gemini ────────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("gemini")
    fun provideGeminiRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideGeminiApiService(@Named("gemini") retrofit: Retrofit): GeminiApiService =
        retrofit.create(GeminiApiService::class.java)

    // ── ImgBB ─────────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("imgbb")
    fun provideImgBBRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.imgbb.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideImgBBApiService(@Named("imgbb") retrofit: Retrofit): ImgBBApiService =
        retrofit.create(ImgBBApiService::class.java)
}