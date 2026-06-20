package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.model.Comida
import com.example.nutrimetrix.domain.repository.IAlimentoRepository
import javax.inject.Inject

/**
 * Caso de uso para registrar una comida creada desde el carrito manual o el escaneo de cámara.
 *
 * Centraliza la lógica de guardado para que los ViewModels [FoodListViewModel] y [CameraViewModel]
 * no dependan directamente del repositorio, siguiendo el principio de Clean Architecture.
 */
class SaveComidaUseCase @Inject constructor(
    private val repository: IAlimentoRepository
) {
    /**
     * Guarda la comida del usuario.
     *
     * @param userId  UID del usuario autenticado.
     * @param comida  Objeto [Comida] con todos los macros y metadatos calculados.
     * @return [Result.success] si se guardó correctamente (local + remoto si hay red),
     *         [Result.failure] si ocurrió un error irrecuperable.
     * @throws IllegalArgumentException si el userId está vacío o la comida no tiene tipo definido.
     */
    suspend operator fun invoke(userId: String, comida: Comida): Result<Unit> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("El ID de usuario no puede estar vacío"))
        }
        if (comida.tipo.isBlank()) {
            return Result.failure(IllegalArgumentException("El tipo de comida no puede estar vacío"))
        }
        return repository.saveComida(userId, comida)
    }
}
