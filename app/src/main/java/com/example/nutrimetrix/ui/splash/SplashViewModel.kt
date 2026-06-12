package com.example.nutrimetrix.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrimetrix.core.navigation.Screen
import com.example.nutrimetrix.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        resolveDestination()
    }

    private fun resolveDestination() {
        viewModelScope.launch {
            _startDestination.value = if (authRepository.getCurrentUserId() != null) {
                Screen.Home.route
            } else {
                Screen.Login.route
            }
        }
    }
}
