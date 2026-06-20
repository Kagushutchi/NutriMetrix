package com.example.nutrimetrix.domain.usecase

import com.example.nutrimetrix.domain.model.Comida
import com.example.nutrimetrix.domain.repository.IAlimentoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Tests unitarios para [GetAlimentosUseCase].
 *
 * Cobertura:
 * - invoke (getComidas): delega al repositorio y emite la lista correcta
 * - getDeHoy: delega a getComidasDeHoy y emite solo las comidas de hoy
 * - getDetalle: retorna la comida por ID desde el repositorio
 * - getDetalle: retorna null cuando la comida no existe
 */
class GetAlimentosUseCaseTest {

    private lateinit var repository: IAlimentoRepository
    private lateinit var useCase: GetAlimentosUseCase

    private fun makeComida(id: String, fecha: String = "2026-06-20") = Comida(
        id            = id,
        userId        = "uid-test",
        nombre        = "Comida $id",
        tipo          = "ALMUERZO",
        totalKcal     = 500.0,
        proteinas     = 30.0,
        carbohidratos = 60.0,
        grasas        = 15.0,
        timestamp     = Date(),
        fecha         = fecha,
        url           = ""
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetAlimentosUseCase(repository)
    }

    // ── invoke (todas las comidas) ────────────────────────────────────────────

    @Test
    fun `invoke - repositorio retorna lista de comidas - use case emite la misma lista`() = runTest {
        // Arrange
        val comidas = listOf(makeComida("1"), makeComida("2"))
        coEvery { repository.getComidas("uid-test") } returns flowOf(comidas)

        // Act
        val emitted = useCase("uid-test").toList()

        // Assert
        assertEquals(1, emitted.size)              // 1 emisión del flow
        assertEquals(2, emitted.first().size)      // 2 comidas en esa emisión
        coVerify(exactly = 1) { repository.getComidas("uid-test") }
    }

    @Test
    fun `invoke - repositorio sin comidas - use case emite lista vacia`() = runTest {
        // Arrange
        coEvery { repository.getComidas("uid-test") } returns flowOf(emptyList())

        // Act
        val emitted = useCase("uid-test").toList()

        // Assert
        assertTrue(emitted.first().isEmpty())
    }

    // ── getDeHoy ─────────────────────────────────────────────────────────────

    @Test
    fun `getDeHoy - repositorio retorna comidas de hoy - use case las emite correctamente`() = runTest {
        // Arrange
        val hoy = listOf(makeComida("10", "2026-06-20"))
        coEvery { repository.getComidasDeHoy("uid-test") } returns flowOf(hoy)

        // Act
        val emitted = useCase.getDeHoy("uid-test").toList()

        // Assert
        assertEquals(1, emitted.size)
        assertEquals("10", emitted.first().first().id)
        coVerify(exactly = 1) { repository.getComidasDeHoy("uid-test") }
    }

    // ── getDetalle ────────────────────────────────────────────────────────────

    @Test
    fun `getDetalle - comida existe - retorna la comida con los datos correctos`() = runTest {
        // Arrange
        val comida = makeComida("abc-123")
        coEvery { repository.getComidaById("uid-test", "abc-123") } returns comida

        // Act
        val result = useCase.getDetalle("uid-test", "abc-123")

        // Assert
        assertNotNull(result)
        assertEquals("abc-123", result!!.id)
        assertEquals("Comida abc-123", result.nombre)
    }

    @Test
    fun `getDetalle - comida no existe - retorna null`() = runTest {
        // Arrange
        coEvery { repository.getComidaById("uid-test", "no-existe") } returns null

        // Act
        val result = useCase.getDetalle("uid-test", "no-existe")

        // Assert
        assertNull(result)
    }
}
