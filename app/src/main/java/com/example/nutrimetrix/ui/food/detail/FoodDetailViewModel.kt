package com.example.nutrimetrix.ui.food.detail

import androidx.lifecycle.SavedStateHandle
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

data class ComidaDetalle(
    val id:        String,
    val tipo:      String,
    val nombre:    String,
    val totalKcal: Double,
    val proteinas: Double,
    val carbos:    Double,
    val grasas:    Double,
    val fecha:     String,
    val hora:      String,
    val imageUrl:  String
)

sealed class FoodDetailUiState {
    object Loading : FoodDetailUiState()
    data class Success(val comida: ComidaDetalle) : FoodDetailUiState()
    data class Error(val message: String) : FoodDetailUiState()
}

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    private val authRepository:      IAuthRepository,
    private val getAlimentosUseCase: GetAlimentosUseCase,
    savedStateHandle:                SavedStateHandle   // lee el foodId de la ruta
) : ViewModel() {

    private val _uiState = MutableStateFlow<FoodDetailUiState>(FoodDetailUiState.Loading)
    val uiState: StateFlow<FoodDetailUiState> = _uiState.asStateFlow()

    // foodId viene del argumento de navegación "food_detail/{foodId}"
    private val foodId: String = checkNotNull(savedStateHandle["foodId"])

    init { cargarDetalle() }

    private fun cargarDetalle() {
        viewModelScope.launch {
            try {
                val uid = authRepository.getCurrentUserId()
                    ?: throw Exception("Usuario no autenticado")

                val comida = getAlimentosUseCase.getDetalle(uid, foodId)
                    ?: throw Exception("Comida no encontrada")

                val fecha = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("es"))
                    .format(comida.timestamp)
                val hora = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(comida.timestamp)

                _uiState.value = FoodDetailUiState.Success(
                    ComidaDetalle(
                        id        = comida.id,
                        tipo      = comida.tipo,
                        nombre    = comida.nombre,
                        totalKcal = comida.totalKcal,
                        proteinas = comida.proteinas,
                        carbos    = comida.carbohidratos,
                        grasas    = comida.grasas,
                        fecha     = fecha,
                        hora      = hora,
                        imageUrl  = comida.url
                    )
                )
            } catch (e: Exception) {
                _uiState.value = FoodDetailUiState.Error(e.message ?: "Error al cargar detalle")
            }
        }
    }
}
