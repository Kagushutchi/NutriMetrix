package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.model.Alimento
import com.example.nutrimetrix.domain.repository.IAlimentoRepository
import javax.inject.Inject

/**
 * Caso de uso para realizar búsquedas de alimentos contra la API de USDA.
 */
class SearchAlimentoUseCase @Inject constructor(
    private val repository: IAlimentoRepository
) {
    suspend operator fun invoke(query: String): List<Alimento> {
        if (query.trim().length < 2) return emptyList()
        return repository.searchAlimentos(query)
    }
}
