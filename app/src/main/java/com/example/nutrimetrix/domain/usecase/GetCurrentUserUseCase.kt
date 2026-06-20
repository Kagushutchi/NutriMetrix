package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.model.User
import com.example.nutrimetrix.domain.repository.IAuthRepository
import javax.inject.Inject

/**
 * Caso de uso para consultar el estado de sesión actual y recuperar el perfil del usuario.
 *
 * Encapsula la lógica de verificación de sesión que se usa en el [SplashViewModel]
 * para decidir si navegar al Home o al Login sin exponer el repositorio directamente.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val repository: IAuthRepository
) {
    /**
     * Retorna el UID del usuario actualmente logueado, o null si no hay sesión activa.
     */
    fun getCurrentUserId(): String? = repository.getCurrentUserId()

    /**
     * Retorna el correo del usuario actualmente logueado, o null si no hay sesión activa.
     */
    fun getCurrentUserEmail(): String? = repository.getCurrentUserEmail()

    /**
     * Verifica si hay una sesión activa. Útil para decisiones de navegación en el Splash.
     */
    fun isLoggedIn(): Boolean = repository.getCurrentUserId() != null

    /**
     * Recupera el perfil completo del usuario autenticado.
     *
     * @return El [User] con todos los datos de perfil, o null si no existe o no hay sesión.
     */
    suspend fun getProfile(): User? {
        val uid = repository.getCurrentUserId() ?: return null
        return repository.getUserProfile(uid)
    }
}
