package com.example.nutrimetrix.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrimetrix.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsUiState {
    object Idle      : SettingsUiState()
    object Loading   : SettingsUiState()
    object LoggedOut : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun cerrarSesion() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                authRepository.signOut().getOrThrow()
                _uiState.value = SettingsUiState.LoggedOut
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Error al cerrar sesión")
            }
        }
    }
}
