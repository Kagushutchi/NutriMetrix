package com.example.nutrimetrix.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrimetrix.domain.repository.IAuthRepository
import com.example.nutrimetrix.domain.usecase.GetAlimentosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// id agregado para poder navegar al detalle desde el Home
data class ComidaResumen(
    val id:        String,
    val nombre:    String,
    val alimentos: String,
    val calorias:  Int
)

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val firstName:          String,
        val caloriasConsumidas: Int,
        val caloriasObjetivo:   Int,
        val proteinasConsumidas:Double,
        val proteinasObjetivo:  Double,
        val carbosConsumidos:   Double,
        val carbosObjetivo:     Double,
        val grasasConsumidas:   Double,
        val grasasObjetivo:     Double,
        val comidas:            List<ComidaResumen>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository:      IAuthRepository,
    private val getAlimentosUseCase: GetAlimentosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { cargarDatosHome() }

    fun cargarDatosHome() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val uid = authRepository.getCurrentUserId()
                    ?: throw Exception("Usuario no autenticado")

                val userProfile = authRepository.getUserProfile(uid)
                    ?: throw Exception("Perfil no encontrado")

                val firstName = userProfile.mail.split("@").firstOrNull() ?: "Usuario"
                val caloriasObjetivo  = userProfile.caloriasDiarias
                val proteinasObjetivo = userProfile.proteinas
                val carbosObjetivo    = userProfile.carbohidratos
                val grasasObjetivo    = userProfile.grasas

                // Escuchar cambios de forma reactiva a través del caso de uso
                getAlimentosUseCase.getDeHoy(uid).collect { comidasList ->
                    var caloriasConsumidas  = 0
                    var proteinasConsumidas = 0.0
                    var carbosConsumidos    = 0.0
                    var grasasConsumidas    = 0.0
                    val listaComidas        = mutableListOf<ComidaResumen>()

                    for (comida in comidasList) {
                        val kcal      = comida.totalKcal.toInt()
                        val proteinas = comida.proteinas
                        val carbos    = comida.carbohidratos
                        val grasas    = comida.grasas
                        val nombre    = comida.nombre
                        val tipo      = comida.tipo

                        caloriasConsumidas  += kcal
                        proteinasConsumidas += proteinas
                        carbosConsumidos    += carbos
                        grasasConsumidas    += grasas

                        listaComidas.add(
                            ComidaResumen(
                                id        = comida.id,
                                nombre    = tipo.replaceFirstChar { it.uppercase() },
                                alimentos = nombre,
                                calorias  = kcal
                            )
                        )
                    }

                    _uiState.value = HomeUiState.Success(
                        firstName           = firstName,
                        caloriasConsumidas  = caloriasConsumidas,
                        caloriasObjetivo    = caloriasObjetivo,
                        proteinasConsumidas = proteinasConsumidas,
                        proteinasObjetivo   = proteinasObjetivo,
                        carbosConsumidos    = carbosConsumidos,
                        carbosObjetivo      = carbosObjetivo,
                        grasasConsumidas    = grasasConsumidas,
                        grasasObjetivo      = grasasObjetivo,
                        comidas             = listaComidas
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error al inicializar datos")
            }
        }
    }
}
