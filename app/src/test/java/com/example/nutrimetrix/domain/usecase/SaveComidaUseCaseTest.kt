package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.model.Comida
import com.example.nutrimetrix.domain.repository.IAlimentoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Tests unitarios para [SaveComidaUseCase].
 *
 * Cobertura:
 * - Datos válidos: delega al repositorio y retorna Result.success
 * - userId vacío: retorna failure con IllegalArgumentException
 * - tipo de comida vacío: retorna failure con IllegalArgumentException
 * - Repositorio falla: el Result.failure se propaga
 * - Repositorio exitoso: coVerify que fue llamado exactamente una vez
 */
class SaveComidaUseCaseTest {

    private lateinit var repository: IAlimentoRepository
    private lateinit var useCase: SaveComidaUseCase

    private fun makeComida(tipo: String = "ALMUERZO") = Comida(
        id            = "",
        userId        = "uid-test",
        nombre        = "Pechuga con arroz",
        tipo          = tipo,
        totalKcal     = 600.0,
        proteinas     = 45.0,
        carbohidratos = 70.0,
        grasas        = 12.0,
        timestamp     = Date(),
        fecha         = "2026-06-20",
        url           = ""
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = SaveComidaUseCase(repository)
    }

    @Test
    fun `invoke - datos validos - delega al repositorio y retorna success`() = runTest {
        // Arrange
        coEvery { repository.saveComida("uid-test", any()) } returns Result.success(Unit)

        // Act
        val result = useCase("uid-test", makeComida())

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.saveComida("uid-test", any()) }
    }

    @Test
    fun `invoke - userId vacio - retorna failure sin llamar al repositorio`() = runTest {
        // Act
        val result = useCase("", makeComida())

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.saveComida(any(), any()) }
    }

    @Test
    fun `invoke - tipo de comida vacio - retorna failure sin llamar al repositorio`() = runTest {
        // Act
        val result = useCase("uid-test", makeComida(tipo = ""))

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.saveComida(any(), any()) }
    }

    @Test
    fun `invoke - repositorio falla - result failure se propaga`() = runTest {
        // Arrange
        coEvery { repository.saveComida("uid-test", any()) } returns Result.failure(Exception("Error de base de datos"))

        // Act
        val result = useCase("uid-test", makeComida())

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Error de base de datos", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke - tipo COLACION en mayusculas - es valido y delega al repositorio`() = runTest {
        // Arrange
        coEvery { repository.saveComida("uid-test", any()) } returns Result.success(Unit)

        // Act
        val result = useCase("uid-test", makeComida(tipo = "COLACIÓN"))

        // Assert
        assertTrue(result.isSuccess)
    }
}
