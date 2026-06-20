package com.example.nutrimetrix.ui.food.list

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.nutrimetrix.domain.model.Alimento
import com.example.nutrimetrix.domain.model.Comida
import com.example.nutrimetrix.domain.repository.IAlimentoRepository
import com.example.nutrimetrix.domain.repository.IAuthRepository
import com.example.nutrimetrix.domain.usecase.SearchAlimentoUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests unitarios para [FoodListViewModel].
 *
 * Cobertura:
 * - buscarAlimentos: debounce + Success con resultados
 * - buscarAlimentos: query muy corta → no lanza búsqueda (permanece Idle)
 * - buscarAlimentos: error en repositorio → estado Error
 * - addToCart / removeFromCart: gestión del carrito
 * - addToCart duplicado: suma gramos en lugar de duplicar
 * - confirmarComida: Loading → Success → navegación
 * - confirmarComida: sin usuario → Error
 * - resetSaveState: vuelve a Idle
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FoodListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var searchAlimentoUseCase: SearchAlimentoUseCase
    private lateinit var alimentoRepository:    IAlimentoRepository
    private lateinit var authRepository:        IAuthRepository
    private lateinit var viewModel: FoodListViewModel

    private val alimentoA = Alimento(fdcId = "1", nombre = "Pechuga de pollo", kcal100g = 165.0, proteina = 31.0, carbo = 0.0, grasa = 3.6)
    private val alimentoB = Alimento(fdcId = "2", nombre = "Arroz blanco",     kcal100g = 130.0, proteina = 2.7, carbo = 28.0, grasa = 0.3)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        searchAlimentoUseCase = mockk()
        alimentoRepository    = mockk()
        authRepository        = mockk()
        viewModel = FoodListViewModel(searchAlimentoUseCase, alimentoRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Búsqueda con debounce ─────────────────────────────────────────────────

    @Test
    fun `buscarAlimentos - query valida despues del debounce - searchState es Success con resultados`() = runTest {
        // Arrange
        coEvery { searchAlimentoUseCase("pollo") } returns listOf(alimentoA)

        // Act: simular escritura del usuario y esperar el debounce de 500ms
        viewModel.onQueryChange("pollo")
        advanceTimeBy(600L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.searchState.value
        assertTrue("Se esperaba Success, pero es: $state", state is SearchUiState.Success)
        val results = (state as SearchUiState.Success).results
        assertEquals(1, results.size)
        assertEquals("Pechuga de pollo", results.first().nombre)
    }

    @Test
    fun `buscarAlimentos - query de 1 caracter - no lanza busqueda, permanece Idle`() = runTest {
        // Act
        viewModel.onQueryChange("p")
        advanceTimeBy(600L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: el UseCase devuelve lista vacía para queries cortas y jamás se llega a Success
        assertTrue(viewModel.searchState.value is SearchUiState.Idle)
    }

    @Test
    fun `buscarAlimentos - repositorio lanza excepcion - searchState es Error`() = runTest {
        // Arrange
        coEvery { searchAlimentoUseCase("xyz") } throws Exception("Sin conexión")

        // Act
        viewModel.onQueryChange("xyz")
        advanceTimeBy(600L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.searchState.value
        assertTrue("Se esperaba Error, pero es: $state", state is SearchUiState.Error)
        assertEquals("Sin conexión", (state as SearchUiState.Error).message)
    }

    // ── Carrito ───────────────────────────────────────────────────────────────

    @Test
    fun `addToCart - alimento nuevo - cart contiene 1 item con 100g`() {
        // Act
        viewModel.addToCart(alimentoA)

        // Assert
        val cart = viewModel.cart.value
        assertEquals(1, cart.size)
        assertEquals("1", cart.first().alimento.fdcId)
        assertEquals(100.0, cart.first().gramos, 0.01)
    }

    @Test
    fun `addToCart - mismo alimento dos veces - cart tiene 1 item con 200g`() {
        // Act
        viewModel.addToCart(alimentoA)
        viewModel.addToCart(alimentoA)

        // Assert
        val cart = viewModel.cart.value
        assertEquals(1, cart.size)
        assertEquals(200.0, cart.first().gramos, 0.01)
    }

    @Test
    fun `addToCart - dos alimentos distintos - cart contiene 2 items`() {
        // Act
        viewModel.addToCart(alimentoA)
        viewModel.addToCart(alimentoB)

        // Assert
        assertEquals(2, viewModel.cart.value.size)
    }

    @Test
    fun `removeFromCart - alimento en carrito - cart queda vacio`() {
        // Arrange
        viewModel.addToCart(alimentoA)

        // Act
        viewModel.removeFromCart("1")

        // Assert
        assertTrue(viewModel.cart.value.isEmpty())
    }

    @Test
    fun `removeFromCart - fdcId inexistente - cart no cambia`() {
        // Arrange
        viewModel.addToCart(alimentoA)

        // Act
        viewModel.removeFromCart("999")

        // Assert
        assertEquals(1, viewModel.cart.value.size)
    }

    @Test
    fun `CartItem - kcal se calculan proporcional a los gramos`() {
        // Arrange: alimentoA tiene 165 kcal/100g
        viewModel.addToCart(alimentoA)

        // Act: actualizar a 200g
        viewModel.updateGramos("1", 200.0)

        // Assert: 165 * 200 / 100 = 330 kcal
        val item = viewModel.cart.value.first()
        assertEquals(330.0, item.kcal, 0.01)
    }

    // ── confirmarComida ───────────────────────────────────────────────────────

    @Test
    fun `confirmarComida - datos validos - saveState llega a Success y llama a onNavigateBack`() = runTest {
        // Arrange
        every { authRepository.getCurrentUserId() } returns "uid-test"
        coEvery { alimentoRepository.saveComida(any(), any()) } returns Result.success(Unit)

        viewModel.addToCart(alimentoA)
        viewModel.onTipoComidaChange("ALMUERZO")

        var navCalled = false

        // Act
        viewModel.confirmarComida { navCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertTrue("El callback de navegación no fue invocado", navCalled)
        // Después de éxito se limpia el carrito
        assertTrue(viewModel.cart.value.isEmpty())
    }

    @Test
    fun `confirmarComida - sin usuario autenticado - saveState llega a Error`() = runTest {
        // Arrange
        every { authRepository.getCurrentUserId() } returns null

        viewModel.addToCart(alimentoA)
        viewModel.onTipoComidaChange("CENA")

        // Act
        viewModel.confirmarComida {}
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.saveState.value
        assertTrue("Se esperaba SaveUiState.Error, pero es: $state", state is SaveUiState.Error)
    }

    @Test
    fun `confirmarComida - carrito vacio - no cambia saveState`() = runTest {
        // Arrange: carrito vacío
        viewModel.onTipoComidaChange("DESAYUNO")

        // Act
        viewModel.confirmarComida {}
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: sin carrito no se dispara la operación
        assertEquals(SaveUiState.Idle, viewModel.saveState.value)
    }

    @Test
    fun `confirmarComida - tipo de comida vacio - no cambia saveState`() = runTest {
        // Arrange: carrito con alimento pero sin tipo de comida
        viewModel.addToCart(alimentoB)
        // tipoComida se deja en ""

        // Act
        viewModel.confirmarComida {}
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: sin tipo de comida no se dispara la operación
        assertEquals(SaveUiState.Idle, viewModel.saveState.value)
    }

    @Test
    fun `confirmarComida - repositorio falla - saveState llega a Error con mensaje`() = runTest {
        // Arrange
        every { authRepository.getCurrentUserId() } returns "uid-test"
        coEvery { alimentoRepository.saveComida(any(), any()) } returns Result.failure(Exception("Error de red"))

        viewModel.addToCart(alimentoA)
        viewModel.onTipoComidaChange("MERIENDA")

        // Act
        viewModel.confirmarComida {}
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.saveState.value
        assertTrue("Se esperaba Error, pero es: $state", state is SaveUiState.Error)
        assertEquals("Error de red", (state as SaveUiState.Error).message)
    }

    // ── resetSaveState ────────────────────────────────────────────────────────

    @Test
    fun `resetSaveState - desde estado Error - vuelve a Idle`() = runTest {
        // Arrange: provocar un estado Error
        every { authRepository.getCurrentUserId() } returns null
        viewModel.addToCart(alimentoA)
        viewModel.onTipoComidaChange("CENA")
        viewModel.confirmarComida {}
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.saveState.value is SaveUiState.Error)

        // Act
        viewModel.resetSaveState()

        // Assert
        assertEquals(SaveUiState.Idle, viewModel.saveState.value)
    }
}
