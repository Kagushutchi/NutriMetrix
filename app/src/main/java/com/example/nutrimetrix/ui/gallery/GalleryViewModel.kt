package com.example.nutrimetrix.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrimetrix.domain.repository.IAuthRepository
import com.example.nutrimetrix.domain.usecase.GetAlimentosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ── Modelo de una comida para mostrar en la galería ───────────────────────────
data class ComidaGaleria(
    val id:         String,
    val tipo:       String,       // DESAYUNO, ALMUERZO, etc.
    val nombre:     String,       // alimentos separados por coma
    val totalKcal:  Double,
    val proteinas:  Double,
    val carbos:     Double,
    val grasas:     Double,
    val fecha:      String,       // "12 Abril"
    val hora:       String,       // "09:30"
    val imageUrl:   String        // vacío hasta que se implemente cámara
)

sealed class GalleryUiState {
    object Loading : GalleryUiState()
    data class Success(val comidas: List<ComidaGaleria>) : GalleryUiState()
    data class Error(val message: String) : GalleryUiState()
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val authRepository:      IAuthRepository,
    private val getAlimentosUseCase: GetAlimentosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init { cargarGaleria() }

    fun cargarGaleria() {
        viewModelScope.launch {
            _uiState.value = GalleryUiState.Loading
            try {
                val uid = authRepository.getCurrentUserId()
                    ?: throw Exception("Usuario no autenticado")

                getAlimentosUseCase(uid).collect { comidasList ->
                    val comidas = comidasList.map { comida ->
                        ComidaGaleria(
                            id        = comida.id,
                            tipo      = comida.tipo,
                            nombre    = comida.nombre,
                            totalKcal = comida.totalKcal,
                            proteinas = comida.proteinas,
                            carbos    = comida.carbohidratos,
                            grasas    = comida.grasas,
                            fecha     = formatFecha(comida.timestamp),
                            hora      = formatHora(comida.timestamp),
                            imageUrl  = comida.url
                        )
                    }
                    _uiState.value = GalleryUiState.Success(comidas)
                }
            } catch (e: Exception) {
                _uiState.value = GalleryUiState.Error(e.message ?: "Error al cargar galería")
            }
        }
    }

    private fun formatFecha(date: Date): String {
        val sdf = SimpleDateFormat("d MMMM", Locale("es"))
        return sdf.format(date)
    }

    private fun formatHora(date: Date): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
}
