package com.example.nutrimetrix.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

data class NutritionResult(
    val caloriasObjetivo: Int,
    val proteinas: Double,
    val carbohidratos: Double,
    val grasas: Double,
    val semanasEstimadas: String
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _registerData = MutableStateFlow(RegisterData())
    val registerData: StateFlow<RegisterData> = _registerData.asStateFlow()

    private val _nutritionResult = MutableStateFlow<NutritionResult?>(null)
    val nutritionResult: StateFlow<NutritionResult?> = _nutritionResult.asStateFlow()

    // ── Google Sign In ────────────────────────────────────────────────────────
    // Después de autenticar con Firebase, consulta Firestore para saber
    // si el usuario ya completó el registro (tiene documento) o es nuevo
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(credential).await()

                val uid = firebaseAuth.currentUser?.uid
                    ?: throw Exception("No se obtuvo UID")

                // Verificar si ya existe en Firestore
                val doc = firestore.collection("usuarios").document(uid).get().await()
                val isNewUser = !doc.exists()

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

        val tmb = if (data.genero == "MASCULINO") {
            (10 * data.peso) + (6.25 * data.altura) - (5 * data.edad) + 5
        } else {
            (10 * data.peso) + (6.25 * data.altura) - (5 * data.edad) - 161
        }

        val factor = when (data.nivelActividad) {
            "SEDENTARIO" -> 1.2
            "LIGERO"     -> 1.375
            "MODERADO"   -> 1.55
            "ACTIVO"     -> 1.725
            "MUY_ACTIVO" -> 1.9
            else         -> 1.2
        }
        val get = tmb * factor

        val caloriasObjetivo = when (data.objetivo) {
            "DEFICIT"    -> get - 500
            "SUPERAVIT"  -> get + 400
            else         -> get
        }

        val proteinas = data.peso * 2.0
        val grasas    = (caloriasObjetivo * 0.25) / 9.0
        val carbos    = (caloriasObjetivo - (proteinas * 4) - (grasas * 9)) / 4.0

        val diferencia = Math.abs(data.peso - data.pesoIdeal)
        val semanasEstimadas = if (diferencia < 1) {
            "Ya estás en tu peso ideal"
        } else {
            "${(diferencia / 0.5).toInt()} semanas aprox."
        }

        _nutritionResult.value = NutritionResult(
            caloriasObjetivo = caloriasObjetivo.toInt(),
            proteinas        = proteinas,
            carbohidratos    = carbos,
            grasas           = grasas,
            semanasEstimadas = semanasEstimadas
        )
    }

    // ── Guardar en Firestore ──────────────────────────────────────────────────
    fun guardarUsuarioEnFirestore(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val uid  = firebaseAuth.currentUser?.uid ?: throw Exception("Sin usuario")
                val data = _registerData.value
                val res  = _nutritionResult.value ?: throw Exception("Sin cálculo")

                val userMap = mapOf(
                    "id"               to uid,
                    "mail"             to (firebaseAuth.currentUser?.email ?: ""),
                    "peso"             to data.peso,
                    "altura"           to data.altura,
                    "edad"             to data.edad,
                    "objetivo"         to data.objetivo,
                    "peso_ideal"       to data.pesoIdeal,
                    "nivel_actividad"  to data.nivelActividad,
                    "genero"           to data.genero,
                    "calorias_diarias" to res.caloriasObjetivo,
                    "proteinas"        to res.proteinas,
                    "carbohidratos"    to res.carbohidratos,
                    "grasas"           to res.grasas
                )

                firestore.collection("usuarios").document(uid).set(userMap).await()
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
