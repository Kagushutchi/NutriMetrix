package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.model.Comida
import com.example.nutrimetrix.domain.repository.IAlimentoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener las comidas registradas del usuario desde el repositorio.
 */
class GetAlimentosUseCase @Inject constructor(
    private val repository: IAlimentoRepository
) {
    /**
     * Retorna la lista total de comidas registradas de un usuario en un flujo continuo.
     */
    operator fun invoke(userId: String): Flow<List<Comida>> {
        return repository.getComidas(userId)
    }

    /**
     * Retorna las comidas registradas para el día de hoy.
     */
    fun getDeHoy(userId: String): Flow<List<Comida>> {
        return repository.getComidasDeHoy(userId)
    }

    /**
     * Obtiene el detalle de una comida específica por su ID.
     */
    suspend fun getDetalle(userId: String, comidaId: String): Comida? {
        return repository.getComidaById(userId, comidaId)
    }
}
