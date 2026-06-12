package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.model.AnalisisComidaIa
import com.example.nutrimetrix.domain.repository.IAlimentoRepository
import javax.inject.Inject

/**
 * Caso de uso para analizar fotos de platillos con IA y obtener su desglose de ingredientes y nutrientes.
 */
class AnalyzeImageUseCase @Inject constructor(
    private val repository: IAlimentoRepository
) {
    /**
     * Analiza una imagen codificada en Base64 y retorna el desglose nutricional calculado.
     */
    suspend operator fun invoke(base64Image: String): Result<AnalisisComidaIa> {
        if (base64Image.isBlank()) {
            return Result.failure(IllegalArgumentException("La imagen en Base64 no puede estar vacía"))
        }
        return repository.analyzeImage(base64Image)
    }
}
