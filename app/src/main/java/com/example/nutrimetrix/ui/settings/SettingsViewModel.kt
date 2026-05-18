package com.example.nutrimetrix.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

sealed class SettingsUiState {
    object Idle      : SettingsUiState()
    object Loading   : SettingsUiState()
    object LoggedOut : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun cerrarSesion() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                // 1. Cerrar sesión en Firebase Auth
                firebaseAuth.signOut()

                // 2. Revocar también la sesión de Google para que
                //    la próxima vez muestre el picker de cuentas
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(context, gso).signOut().await()

                _uiState.value = SettingsUiState.LoggedOut
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Error al cerrar sesión")
            }
        }
    }
}
