package com.example.nutrimetrix.ui.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.nutrimetrix.domain.model.Comida
import com.example.nutrimetrix.domain.model.User
import com.example.nutrimetrix.domain.repository.IAuthRepository
import com.example.nutrimetrix.domain.usecase.GetAlimentosUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * Tests unitarios para [HomeViewModel].
 *
 * Cobertura:
 * - cargarDatosHome: Loading → Success cuando repositorios responden OK
 * - cargarDatosHome: acumula calorías, macros y lista de comidas correctamente
 * - cargarDatosHome: Loading → Error cuando no hay usuario autenticado
 * - cargarDatosHome: Loading → Error cuando no se encuentra el perfil
 * - lista vacía: suma cero en calorías y macros
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: IAuthRepository
    private lateinit var getAlimentosUseCase: GetAlimentosUseCase
    private lateinit var viewModel: HomeViewModel

    // Perfil de usuario de prueba
    private val testUser = User(
        id              = "uid-test",
        mail            = "usuario@test.com",
        peso            = 75.0,
        altura          = 175,
        edad            = 28,
        genero          = "MASCULINO",
        objetivo        = "MANTENIMIENTO",
        nivelActividad  = "MODERADO",
        pesoIdeal       = 75.0,
        caloriasDiarias = 2500,
        proteinas       = 150.0,
        carbohidratos   = 300.0,
        grasas          = 70.0
    )

    private fun makeComida(id: String, nombre: String, kcal: Double, prot: Double, carbs: Double, grasas: Double) = Comida(
        id            = id,
        userId        = "uid-test",
        nombre        = nombre,
        tipo          = "ALMUERZO",
        totalKcal     = kcal,
        proteinas     = prot,
        carbohidratos = carbs,
        grasas        = grasas,
        timestamp     = Date(),
        fecha         = "2026-06-20",
        url           = ""
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository      = mockk()
        getAlimentosUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildViewModel(): HomeViewModel = HomeViewModel(authRepository, getAlimentosUseCase)

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `cargarDatosHome - repositorios OK y comidas del dia - estado llega a Success`() = runTest {
        // Arrange
        every { authRepository.getCurrentUserId() } returns "uid-test"
        coEvery { authRepository.getUserProfile("uid-test") } returns testUser

        val comidas = listOf(
            makeComida("1", "Pollo con arroz", 600.0, 40.0, 60.0, 15.0),
            makeComida("2", "Ensalada cesar",  250.0, 10.0, 20.0,  8.0)
        )
        every { getAlimentosUseCase.getDeHoy("uid-test") } returns flowOf(comidas)

        // Act
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue("Se esperaba Success, pero es: $state", state is HomeUiState.Success)
    }

    @Test
    fun `cargarDatosHome - dos comidas - calorias totales son la suma correcta`() = runTest {
        // Arrange
        every { authRepository.getCurrentUserId() } returns "uid-test"
        coEvery { authRepository.getUserProfile("uid-test") } returns testUser

        val comidas = listOf(
            makeComida("1", "Desayuno", 400.0, 20.0, 50.0, 10.0),
            makeComida("2", "Almuerzo", 700.0, 45.0, 80.0, 20.0)
        )
        every { getAlimentosUseCase.getDeHoy("uid-test") } returns flowOf(comidas)

        // Act
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(1100, state.caloriasConsumidas)
        assertEquals(65.0, state.proteinasConsumidas, 0.01)
        assertEquals(130.0, state.carbosConsumidos, 0.01)
        assertEquals(30.0, state.grasasConsumidas, 0.01)
    }

    @Test
    fun `cargarDatosHome - sin comidas hoy - calorias y macros en cero y lista vacia`() = runTest {
        // Arrange
        every { authRepository.getCurrentUserId() } returns "uid-test"
        coEvery { authRepository.getUserProfile("uid-test") } returns testUser
        every { getAlimentosUseCase.getDeHoy("uid-test") } returns flowOf(emptyList())

        // Act
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(0, state.caloriasConsumidas)
        assertEquals(0.0, state.proteinasConsumidas, 0.01)
        assertTrue(state.comidas.isEmpty())
    }

    @Test
    fun `cargarDatosHome - comida agrega resumen con tipo capitalizado`() = runTest {
        // Arrange
        every { authRepository.getCurrentUserId() } returns "uid-test"
        coEvery { authRepository.getUserProfile("uid-test") } returns testUser

        val comida = makeComida("1", "Tarta de jamón", 500.0, 25.0, 60.0, 18.0).copy(tipo = "CENA")
        every { getAlimentosUseCase.getDeHoy("uid-test") } returns flowOf(listOf(comida))

        // Act
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(1, state.comidas.size)
        // El tipo "CENA" debe aparecer capitalizado como "Cena"
        assertEquals("Cena", state.comidas.first().nombre)
        assertEquals("Tarta de jamón", state.comidas.first().alimentos)
        assertEquals(500, state.comidas.first().calorias)
    }

    @Test
    fun `cargarDatosHome - sin usuario autenticado - uiState llega a Error`() = runTest {
        // Arrange: sin sesión
        every { authRepository.getCurrentUserId() } returns null

        // Act
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue("Se esperaba Error, pero es: $state", state is HomeUiState.Error)
    }

    @Test
    fun `cargarDatosHome - perfil no encontrado - uiState llega a Error`() = runTest {
        // Arrange
        every { authRepository.getCurrentUserId() } returns "uid-test"
        coEvery { authRepository.getUserProfile("uid-test") } returns null

        // Act
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue("Se esperaba Error, pero es: $state", state is HomeUiState.Error)
    }

    @Test
    fun `cargarDatosHome - calorías objetivo vienen del perfil del usuario`() = runTest {
        // Arrange
        every { authRepository.getCurrentUserId() } returns "uid-test"
        coEvery { authRepository.getUserProfile("uid-test") } returns testUser.copy(caloriasDiarias = 2200)
        every { getAlimentosUseCase.getDeHoy("uid-test") } returns flowOf(emptyList())

        // Act
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(2200, state.caloriasObjetivo)
    }
}
