package com.example.nutrimetrix.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val peso: Double,
        val altura: Int,
        val nivelActividad: String,
        val pesoIdeal: Double,
        val objetivo: String,
        val semanasTranscurridas: Int,
        val semanasTotal: Int
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        cargarPerfil()
    }

     fun cargarPerfil() {
        viewModelScope.launch {
            try {
                val uid = firebaseAuth.currentUser?.uid
                    ?: throw Exception("Usuario no autenticado")

                val doc = firestore.collection("usuarios").document(uid).get().await()

                val peso           = doc.getDouble("peso")          ?: 0.0
                val altura         = (doc.getLong("altura")         ?: 0L).toInt()
                val nivelActividad = doc.getString("nivel_actividad") ?: ""
                val pesoIdeal      = doc.getDouble("peso_ideal")    ?: 0.0
                val objetivo       = doc.getString("objetivo")      ?: ""

                // Semanas transcurridas desde el registro
                // Por ahora usamos 0 — en feature/room se puede guardar la fecha de inicio
                val semanasTranscurridas = 0

                // Semanas totales estimadas según diferencia de peso
                val diferencia    = Math.abs(peso - pesoIdeal)
                val semanasTotal  = if (diferencia < 1) 0 else (diferencia / 0.5).toInt()

                _uiState.value = ProfileUiState.Success(
                    peso                 = peso,
                    altura               = altura,
                    nivelActividad       = nivelActividad,
                    pesoIdeal            = pesoIdeal,
                    objetivo             = objetivo,
                    semanasTranscurridas = semanasTranscurridas,
                    semanasTotal         = semanasTotal
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Error al cargar perfil")
            }
        }
    }
}
