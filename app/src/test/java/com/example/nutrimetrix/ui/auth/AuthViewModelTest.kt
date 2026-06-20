package com.example.nutrimetrix.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.nutrimetrix.domain.model.User
import com.example.nutrimetrix.domain.repository.IAuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests unitarios para [AuthViewModel].
 *
 * Cobertura:
 * - signInWithGoogle: Idle → Loading → Success (usuario nuevo)
 * - signInWithGoogle: Idle → Loading → Success (usuario existente)
 * - signInWithGoogle: Idle → Loading → Error
 * - guardarUsuarioEnFirestore: Loading → Idle + callback de éxito
 * - guardarUsuarioEnFirestore: sin usuario → Error
 * - calcularNutricion: verifica que User.calcularNutricion produce valores coherentes
 * - resetState: vuelve al estado Idle
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    // Sincroniza LiveData en el hilo principal durante los tests
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: IAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── signInWithGoogle ──────────────────────────────────────────────────────

    @Test
    fun `signInWithGoogle - usuario nuevo - pasa por Loading y llega a Success con isNewUser true`() = runTest {
        // Arrange
        coEvery { authRepository.signInWithGoogle(any()) } returns Result.success(true)

        // Act
        viewModel.signInWithGoogle("fake-id-token")

        // Avanzar corrutinas pendientes
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue("Esperaba Success, pero es: $state", state is AuthUiState.Success)
        assertTrue((state as AuthUiState.Success).isNewUser)
    }

    @Test
    fun `signInWithGoogle - usuario existente - llega a Success con isNewUser false`() = runTest {
        // Arrange
        coEvery { authRepository.signInWithGoogle(any()) } returns Result.success(false)

        // Act
        viewModel.signInWithGoogle("fake-id-token")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Success)
        assertTrue(!(state as AuthUiState.Success).isNewUser)
    }

    @Test
    fun `signInWithGoogle - error de red - uiState llega a Error con mensaje correcto`() = runTest {
        // Arrange
        val errorMsg = "Token inválido"
        coEvery { authRepository.signInWithGoogle(any()) } returns Result.failure(Exception(errorMsg))

        // Act
        viewModel.signInWithGoogle("bad-token")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals(errorMsg, (state as AuthUiState.Error).message)
    }

    // ── guardarUsuarioEnFirestore ─────────────────────────────────────────────

    @Test
    fun `guardarUsuarioEnFirestore - datos validos - llama callback y pasa a Idle`() = runTest {
        // Arrange: configurar repositorio mock
        every { authRepository.getCurrentUserId() } returns "uid-123"
        every { authRepository.getCurrentUserEmail() } returns "test@example.com"
        coEvery { authRepository.saveUserProfile(any()) } returns Result.success(Unit)

        // Cargar datos en el ViewModel simulando los pasos del onboarding
        viewModel.updateStep2(peso = 75.0, altura = 175, edad = 28, genero = "MASCULINO")
        viewModel.updateStep3(objetivo = "MANTENIMIENTO")
        viewModel.updateStep4(pesoIdeal = 75.0, nivelActividad = "MODERADO")

        var callbackExecuted = false

        // Act
        viewModel.guardarUsuarioEnFirestore { callbackExecuted = true }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertTrue("El callback onSuccess no fue invocado", callbackExecuted)
        assertTrue(
            "Se esperaba AuthUiState.Idle después del guardado, pero es: ${viewModel.uiState.value}",
            viewModel.uiState.value is AuthUiState.Idle
        )
    }

    @Test
    fun `guardarUsuarioEnFirestore - sin usuario autenticado - uiState llega a Error`() = runTest {
        // Arrange: UID nulo simula no estar autenticado
        every { authRepository.getCurrentUserId() } returns null

        viewModel.updateStep2(peso = 70.0, altura = 170, edad = 25, genero = "FEMENINO")
        viewModel.updateStep3(objetivo = "DEFICIT")
        viewModel.updateStep4(pesoIdeal = 60.0, nivelActividad = "LIGERO")

        // Act
        viewModel.guardarUsuarioEnFirestore {}
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue("Se esperaba Error, pero es: $state", state is AuthUiState.Error)
    }

    // ── User.calcularNutricion ────────────────────────────────────────────────

    @Test
    fun `calcularNutricion - hombre moderado mantenimiento - calorias mayores a cero y macros coherentes`() {
        // Act
        val result = User.calcularNutricion(
            peso           = 80.0,
            altura         = 180,
            edad           = 30,
            genero         = "MASCULINO",
            nivelActividad = "MODERADO",
            objetivo       = "MANTENIMIENTO",
            pesoIdeal      = 78.0
        )

        // Assert
        assertTrue("Calorías objetivo deben ser positivas", result.caloriasObjetivo > 0)
        assertTrue("Proteínas deben ser positivas", result.proteinas > 0)
        assertTrue("Carbohidratos deben ser positivos", result.carbohidratos > 0)
        assertTrue("Grasas deben ser positivas", result.grasas > 0)
        assertNotNull("Semanas estimadas no debe ser nulo", result.semanasEstimadas)
    }

    @Test
    fun `calcularNutricion - objetivo DEFICIT - calorias menores que MANTENIMIENTO`() {
        // Act
        val mantenimiento = User.calcularNutricion(
            peso = 75.0, altura = 170, edad = 25, genero = "FEMENINO",
            nivelActividad = "LIGERO", objetivo = "MANTENIMIENTO", pesoIdeal = 65.0
        )
        val deficit = User.calcularNutricion(
            peso = 75.0, altura = 170, edad = 25, genero = "FEMENINO",
            nivelActividad = "LIGERO", objetivo = "DEFICIT", pesoIdeal = 65.0
        )

        // Assert: déficit debe tener menos calorías
        assertTrue(
            "Déficit (${deficit.caloriasObjetivo}) debería ser menor a mantenimiento (${mantenimiento.caloriasObjetivo})",
            deficit.caloriasObjetivo < mantenimiento.caloriasObjetivo
        )
    }

    @Test
    fun `calcularNutricion - objetivo SUPERAVIT - calorias mayores que MANTENIMIENTO`() {
        // Act
        val mantenimiento = User.calcularNutricion(
            peso = 70.0, altura = 175, edad = 22, genero = "MASCULINO",
            nivelActividad = "ACTIVO", objetivo = "MANTENIMIENTO", pesoIdeal = 72.0
        )
        val superavit = User.calcularNutricion(
            peso = 70.0, altura = 175, edad = 22, genero = "MASCULINO",
            nivelActividad = "ACTIVO", objetivo = "SUPERAVIT", pesoIdeal = 72.0
        )

        // Assert
        assertTrue(
            "Superávit (${superavit.caloriasObjetivo}) debería ser mayor a mantenimiento (${mantenimiento.caloriasObjetivo})",
            superavit.caloriasObjetivo > mantenimiento.caloriasObjetivo
        )
    }

    @Test
    fun `calcularNutricion - peso ya en peso ideal - semanasEstimadas indica que ya esta en peso ideal`() {
        // Act
        val result = User.calcularNutricion(
            peso = 70.0, altura = 170, edad = 28, genero = "FEMENINO",
            nivelActividad = "MODERADO", objetivo = "MANTENIMIENTO", pesoIdeal = 70.0
        )

        // Assert
        assertEquals("Ya estás en tu peso ideal", result.semanasEstimadas)
    }

    // ── resetState ────────────────────────────────────────────────────────────

    @Test
    fun `resetState - desde estado Error - vuelve a Idle`() = runTest {
        // Arrange: llevar a estado de error
        coEvery { authRepository.signInWithGoogle(any()) } returns Result.failure(Exception("Error"))
        viewModel.signInWithGoogle("bad-token")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AuthUiState.Error)

        // Act
        viewModel.resetState()

        // Assert
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }
}
