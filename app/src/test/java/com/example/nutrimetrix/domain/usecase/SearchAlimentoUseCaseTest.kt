package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.model.Alimento
import com.example.nutrimetrix.domain.repository.IAlimentoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios para [SearchAlimentoUseCase].
 *
 * Cobertura:
 * - Query válida: delega al repositorio y retorna resultados
 * - Query de 1 carácter: retorna lista vacía sin llamar al repositorio
 * - Query vacía: retorna lista vacía sin llamar al repositorio
 * - Query con espacios: retorna lista vacía sin llamar al repositorio
 * - Repositorio lanza excepción: la excepción se propaga
 */
class SearchAlimentoUseCaseTest {

    private lateinit var repository: IAlimentoRepository
    private lateinit var useCase: SearchAlimentoUseCase

    private val alimentosMock = listOf(
        Alimento(fdcId = "1", nombre = "Pollo asado", kcal100g = 239.0, proteina = 27.3, carbo = 0.0, grasa = 13.6),
        Alimento(fdcId = "2", nombre = "Pollo a la plancha", kcal100g = 165.0, proteina = 31.0, carbo = 0.0, grasa = 3.6)
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = SearchAlimentoUseCase(repository)
    }

    @Test
    fun `invoke - query de 3 caracteres - delega al repositorio y retorna resultados`() = runTest {
        // Arrange
        coEvery { repository.searchAlimentos("pol") } returns alimentosMock

        // Act
        val result = useCase("pol")

        // Assert
        assertEquals(2, result.size)
        assertEquals("Pollo asado", result.first().nombre)
        coVerify(exactly = 1) { repository.searchAlimentos("pol") }
    }

    @Test
    fun `invoke - query de 1 caracter - retorna lista vacia sin llamar al repositorio`() = runTest {
        // Act
        val result = useCase("p")

        // Assert
        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { repository.searchAlimentos(any()) }
    }

    @Test
    fun `invoke - query vacia - retorna lista vacia sin llamar al repositorio`() = runTest {
        // Act
        val result = useCase("")

        // Assert
        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { repository.searchAlimentos(any()) }
    }

    @Test
    fun `invoke - query de solo espacios - retorna lista vacia sin llamar al repositorio`() = runTest {
        // Arrange: "  " tiene length 2 pero trim() la deja en "" (length 0)
        val result = useCase("  ")

        // Assert
        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { repository.searchAlimentos(any()) }
    }

    @Test
    fun `invoke - repositorio retorna lista vacia - use case retorna lista vacia`() = runTest {
        // Arrange
        coEvery { repository.searchAlimentos("xyz") } returns emptyList()

        // Act
        val result = useCase("xyz")

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test(expected = Exception::class)
    fun `invoke - repositorio lanza excepcion - excepcion se propaga al caller`() = runTest {
        // Arrange
        coEvery { repository.searchAlimentos("pollo") } throws Exception("Sin conexión")

        // Act — debe lanzar la excepción
        useCase("pollo")
    }
}
