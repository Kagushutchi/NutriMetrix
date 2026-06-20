package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.model.User
import com.example.nutrimetrix.domain.repository.IAuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios para [GetCurrentUserUseCase].
 */
class GetCurrentUserUseCaseTest {

    private lateinit var repository: IAuthRepository
    private lateinit var useCase: GetCurrentUserUseCase

    private val userMock = User(
        id = "uid-test",
        mail = "test@example.com",
        peso = 70.0,
        altura = 175,
        edad = 25,
        genero = "MASCULINO",
        objetivo = "DEFICIT",
        nivelActividad = "MODERADO",
        pesoIdeal = 68.0,
        caloriasDiarias = 2000,
        proteinas = 140.0,
        carbohidratos = 200.0,
        grasas = 60.0
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetCurrentUserUseCase(repository)
    }

    @Test
    fun `getCurrentUserId - con sesion activa - retorna el UID del repositorio`() {
        // Arrange
        every { repository.getCurrentUserId() } returns "uid-test"

        // Act
        val result = useCase.getCurrentUserId()

        // Assert
        assertEquals("uid-test", result)
    }

    @Test
    fun `getCurrentUserId - sin sesion activa - retorna null`() {
        // Arrange
        every { repository.getCurrentUserId() } returns null

        // Act
        val result = useCase.getCurrentUserId()

        // Assert
        assertNull(result)
    }

    @Test
    fun `getCurrentUserEmail - con sesion activa - retorna el mail del repositorio`() {
        // Arrange
        every { repository.getCurrentUserEmail() } returns "test@example.com"

        // Act
        val result = useCase.getCurrentUserEmail()

        // Assert
        assertEquals("test@example.com", result)
    }

    @Test
    fun `getCurrentUserEmail - sin sesion activa - retorna null`() {
        // Arrange
        every { repository.getCurrentUserEmail() } returns null

        // Act
        val result = useCase.getCurrentUserEmail()

        // Assert
        assertNull(result)
    }

    @Test
    fun `isLoggedIn - con sesion activa - retorna true`() {
        // Arrange
        every { repository.getCurrentUserId() } returns "uid-test"

        // Act
        val result = useCase.isLoggedIn()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isLoggedIn - sin sesion activa - retorna false`() {
        // Arrange
        every { repository.getCurrentUserId() } returns null

        // Act
        val result = useCase.isLoggedIn()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `getProfile - con sesion activa y perfil existente - retorna el perfil`() = runTest {
        // Arrange
        every { repository.getCurrentUserId() } returns "uid-test"
        coEvery { repository.getUserProfile("uid-test") } returns userMock

        // Act
        val result = useCase.getProfile()

        // Assert
        assertEquals(userMock, result)
        coVerify(exactly = 1) { repository.getUserProfile("uid-test") }
    }

    @Test
    fun `getProfile - sin sesion activa - retorna null sin buscar perfil`() = runTest {
        // Arrange
        every { repository.getCurrentUserId() } returns null

        // Act
        val result = useCase.getProfile()

        // Assert
        assertNull(result)
        coVerify(exactly = 0) { repository.getUserProfile(any()) }
    }

    @Test
    fun `getProfile - con sesion activa pero sin perfil guardado - retorna null`() = runTest {
        // Arrange
        every { repository.getCurrentUserId() } returns "uid-test"
        coEvery { repository.getUserProfile("uid-test") } returns null

        // Act
        val result = useCase.getProfile()

        // Assert
        assertNull(result)
    }
}
