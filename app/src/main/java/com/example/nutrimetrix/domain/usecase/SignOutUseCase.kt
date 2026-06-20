package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.repository.IAuthRepository
import javax.inject.Inject

/**
 * Caso de uso para realizar el cierre de sesión completo en Firebase y Google Sign-In.
 *
 * Centraliza la lógica de sign-out para que el [SettingsViewModel] o cualquier
 * otra pantalla no dependa directamente del repositorio.
 */
class SignOutUseCase @Inject constructor(
    private val repository: IAuthRepository
) {
    /**
     * Ejecuta el cierre de sesión.
     *
     * Invalida la sesión de Firebase Auth y revoca el token de Google Sign-In
     * para asegurarse de que el usuario deba volver a autenticarse en el próximo acceso.
     *
     * @return [Result.success] si el cierre de sesión fue exitoso.
     *         [Result.failure] si ocurrió un error durante el proceso.
     */
    suspend operator fun invoke(): Result<Unit> {
        return repository.signOut()
    }
}
