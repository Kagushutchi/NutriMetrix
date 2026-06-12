package com.example.nutrimetrix.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrimetrix.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        cargarPerfil()
    }

     fun cargarPerfil() {
        viewModelScope.launch {
            try {
                val uid = authRepository.getCurrentUserId()
                    ?: throw Exception("Usuario no autenticado")

                val userProfile = authRepository.getUserProfile(uid)
                    ?: throw Exception("Perfil no encontrado")

                val peso           = userProfile.peso
                val altura         = userProfile.altura
                val nivelActividad = userProfile.nivelActividad
                val pesoIdeal      = userProfile.pesoIdeal
                val objetivo       = userProfile.objetivo

                // Semanas transcurridas desde el registro
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
