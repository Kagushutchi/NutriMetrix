package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.model.AnalisisComidaIa
import com.example.nutrimetrix.domain.model.IngredienteDetectado
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
 * Tests unitarios para [AnalyzeImageUseCase].
 *
 * Cobertura:
 * - Base64 válida: delega al repositorio y retorna Result.success con el análisis
 * - Base64 vacía: retorna Result.failure con IllegalArgumentException sin llamar al repositorio
 * - Base64 en blanco: retorna Result.failure con IllegalArgumentException
 * - Repositorio falla: el Result.failure se propaga correctamente
 */
class AnalyzeImageUseCaseTest {

    private lateinit var repository: IAlimentoRepository
    private lateinit var useCase: AnalyzeImageUseCase

    private val analisisMock = AnalisisComidaIa(
        descripcion   = "Pechuga de pollo con arroz",
        ingredientes  = listOf(
            IngredienteDetectado("Pechuga de pollo", 200.0),
            IngredienteDetectado("Arroz blanco cocido", 150.0)
        ),
        totalKcal     = 528.0,
        proteinas     = 68.0,
        carbohidratos = 40.5,
        grasas        = 7.8
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = AnalyzeImageUseCase(repository)
    }

    @Test
    fun `invoke - base64 valida - delega al repositorio y retorna success con analisis`() = runTest {
        // Arrange
        val base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        coEvery { repository.analyzeImage(base64) } returns Result.success(analisisMock)

        // Act
        val result = useCase(base64)

        // Assert
        assertTrue(result.isSuccess)
        val analisis = result.getOrThrow()
        assertEquals("Pechuga de pollo con arroz", analisis.descripcion)
        assertEquals(528.0, analisis.totalKcal, 0.01)
        assertEquals(2, analisis.ingredientes.size)
        coVerify(exactly = 1) { repository.analyzeImage(base64) }
    }

    @Test
    fun `invoke - base64 vacia - retorna failure con IllegalArgumentException sin llamar repositorio`() = runTest {
        // Act
        val result = useCase("")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.analyzeImage(any()) }
    }

    @Test
    fun `invoke - base64 solo espacios - retorna failure con IllegalArgumentException`() = runTest {
        // Act
        val result = useCase("   ")

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.analyzeImage(any()) }
    }

    @Test
    fun `invoke - repositorio falla - result failure se propaga al caller`() = runTest {
        // Arrange
        val base64 = "dGVzdA=="
        coEvery { repository.analyzeImage(base64) } returns Result.failure(Exception("Timeout de Gemini"))

        // Act
        val result = useCase(base64)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Timeout de Gemini", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke - analisis exitoso - ingredientes mapeados correctamente`() = runTest {
        // Arrange
        val base64 = "dGVzdA=="
        coEvery { repository.analyzeImage(base64) } returns Result.success(analisisMock)

        // Act
        val analisis = useCase(base64).getOrThrow()

        // Assert
        assertEquals("Pechuga de pollo", analisis.ingredientes[0].nombre)
        assertEquals(200.0, analisis.ingredientes[0].gramos, 0.01)
        assertEquals("Arroz blanco cocido", analisis.ingredientes[1].nombre)
        assertEquals(150.0, analisis.ingredientes[1].gramos, 0.01)
    }
}
