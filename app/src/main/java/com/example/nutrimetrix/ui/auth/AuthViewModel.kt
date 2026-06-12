package com.example.nutrimetrix.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrimetrix.domain.model.User
import com.example.nutrimetrix.domain.model.NutritionResult
import com.example.nutrimetrix.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val isNewUser: Boolean) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

data class RegisterData(
    val peso: Double = 0.0,
    val altura: Int = 0,
    val edad: Int = 0,
    val genero: String = "",
    val objetivo: String = "",
    val nivelActividad: String = "",
    val pesoIdeal: Double = 0.0
)

// Se utiliza com.example.nutrimetrix.domain.model.NutritionResult del dominio

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: IAuthRepository
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _registerData = MutableStateFlow(RegisterData())
    val registerData: StateFlow<RegisterData> = _registerData.asStateFlow()

    private val _nutritionResult = MutableStateFlow<NutritionResult?>(null)
    val nutritionResult: StateFlow<NutritionResult?> = _nutritionResult.asStateFlow()

    // ── Google Sign In ────────────────────────────────────────────────────────
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val isNewUser = authRepository.signInWithGoogle(idToken).getOrThrow()
                _uiState.value = AuthUiState.Success(isNewUser = isNewUser)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Error al iniciar sesión")
            }
        }
    }

    // ── Actualizadores de pasos ───────────────────────────────────────────────
    fun updateStep2(peso: Double, altura: Int, edad: Int, genero: String) {
        _registerData.value = _registerData.value.copy(
            peso = peso, altura = altura, edad = edad, genero = genero
        )
    }

    fun updateStep3(objetivo: String) {
        _registerData.value = _registerData.value.copy(objetivo = objetivo)
    }

    fun updateStep4(pesoIdeal: Double, nivelActividad: String) {
        _registerData.value = _registerData.value.copy(
            pesoIdeal = pesoIdeal, nivelActividad = nivelActividad
        )
        calcularNutricion()
    }

    // ── Cálculo nutricional ───────────────────────────────────────────────────
    private fun calcularNutricion() {
        val data = _registerData.value
        _nutritionResult.value = User.calcularNutricion(
            peso = data.peso,
            altura = data.altura,
            edad = data.edad,
            genero = data.genero,
            nivelActividad = data.nivelActividad,
            objetivo = data.objetivo,
            pesoIdeal = data.pesoIdeal
        )
    }

    // ── Guardar en Firestore ──────────────────────────────────────────────────
    fun guardarUsuarioEnFirestore(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val uid   = authRepository.getCurrentUserId() ?: throw Exception("Sin usuario")
                val email = authRepository.getCurrentUserEmail() ?: ""
                val data  = _registerData.value
                val res   = _nutritionResult.value ?: throw Exception("Sin cálculo")

                val user = User(
                    id              = uid,
                    mail            = email,
                    peso            = data.peso,
                    altura          = data.altura,
                    edad            = data.edad,
                    genero          = data.genero,
                    objetivo        = data.objetivo,
                    nivelActividad  = data.nivelActividad,
                    pesoIdeal       = data.pesoIdeal,
                    caloriasDiarias = res.caloriasObjetivo,
                    proteinas       = res.proteinas,
                    carbohidratos   = res.carbohidratos,
                    grasas          = res.grasas
                )

                authRepository.saveUserProfile(user).getOrThrow()
                _uiState.value = AuthUiState.Idle  // ← Idle, no Success
                onSuccess()                         // ← navega solo una vez
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Error al guardar")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
