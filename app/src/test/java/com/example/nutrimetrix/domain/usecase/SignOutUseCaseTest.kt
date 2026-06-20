package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.repository.IAuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios para [SignOutUseCase].
 */
class SignOutUseCaseTest {

    private lateinit var repository: IAuthRepository
    private lateinit var useCase: SignOutUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = SignOutUseCase(repository)
    }

    @Test
    fun `invoke - cierre de sesion exitoso - retorna success`() = runTest {
        // Arrange
        coEvery { repository.signOut() } returns Result.success(Unit)

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.signOut() }
    }

    @Test
    fun `invoke - repositorio falla - retorna failure con la excepcion correspondiente`() = runTest {
        // Arrange
        val expectedException = Exception("Error al cerrar sesión en Firebase")
        coEvery { repository.signOut() } returns Result.failure(expectedException)

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
        coVerify(exactly = 1) { repository.signOut() }
    }
}
