package com.example.nutrimetrix.data.remote.api

import com.example.nutrimetrix.data.remote.dto.UsdaSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Endpoint único que uso hasta ahora. de la API USDA Food Data Central.
 * Documentación: https://fdc.nal.usda.gov/fdc-app.html#/?query=
 *
 * Solo busco alimentos en crudo o procesados (Foundation + SR Legacy)
 * para obtener valores nutricionales confiables por 100g.
 */
interface UsdaApiService {

    @GET("fdc/v1/foods/search")
    suspend fun searchFoods(
        @Query("api_key")  apiKey:   String,
        @Query("query")    query:    String,
        @Query("dataType") dataType: String = "Foundation,SR Legacy",
        @Query("pageSize") pageSize: Int    = 20
    ): UsdaSearchResponse
}
