package com.example.nutrimetrix.domain.repository

import com.example.nutrimetrix.domain.model.User

/**
 * Interfaz del repositorio de autenticación y perfil de usuario.
 */
interface IAuthRepository {
    
    /**
     * Autentica con las credenciales de Google usando su idToken.
     * Retorna un Result con un booleano indicando si es un usuario nuevo (true) o existente (false).
     */
    suspend fun signInWithGoogle(idToken: String): Result<Boolean>

    /**
     * Guarda la información del perfil del usuario.
     */
    suspend fun saveUserProfile(user: User): Result<Unit>

    /**
     * Obtiene el perfil de un usuario por su identificador único.
     */
    suspend fun getUserProfile(userId: String): User?

    /**
     * Retorna el UID del usuario actual logueado, o null si no hay sesión activa.
     */
    fun getCurrentUserId(): String?

    /**
     * Retorna el correo del usuario actual logueado, o vacío si no hay sesión activa.
     */
    fun getCurrentUserEmail(): String?

    /**
     * Retorna el nombre visible del usuario actual logueado (ej: de Google Auth), o null si no hay sesión activa.
     */
    fun getCurrentUserName(): String?

    /**
     * Cierra la sesión del usuario actual en Firebase y Google Sign-In.
     */
    suspend fun signOut(): Result<Unit>
}
