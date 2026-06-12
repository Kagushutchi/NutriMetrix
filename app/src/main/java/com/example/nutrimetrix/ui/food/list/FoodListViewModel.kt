package com.example.nutrimetrix.ui.food.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrimetrix.BuildConfig
import com.example.nutrimetrix.domain.model.Alimento
import com.example.nutrimetrix.domain.model.Comida
import com.example.nutrimetrix.domain.repository.IAlimentoRepository
import com.example.nutrimetrix.domain.repository.IAuthRepository
import com.example.nutrimetrix.domain.usecase.SearchAlimentoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject



// ── Item en el carrito (alimento + gramos) ────────────────────────────────────
data class CartItem(
    val alimento: Alimento,
    val gramos: Double = 100.0
) {
    val kcal:     Double get() = alimento.kcal100g * gramos / 100
    val proteinas:Double get() = alimento.proteina  * gramos / 100
    val carbos:   Double get() = alimento.carbo     * gramos / 100
    val grasas:   Double get() = alimento.grasa     * gramos / 100
}

// ── Estados ───────────────────────────────────────────────────────────────────
sealed class SearchUiState {
    object Idle    : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<Alimento>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

sealed class SaveUiState {
    object Idle    : SaveUiState()
    object Loading : SaveUiState()
    object Success : SaveUiState()
    data class Error(val message: String) : SaveUiState()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class FoodListViewModel @Inject constructor(
    private val searchAlimentoUseCase: SearchAlimentoUseCase,
    private val alimentoRepository:    IAlimentoRepository,
    private val authRepository:        IAuthRepository
) : ViewModel() {

    // ── Búsqueda ──────────────────────────────────────────────────────────────
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    // ── Carrito ───────────────────────────────────────────────────────────────
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // ── Tipo de comida ────────────────────────────────────────────────────────
    private val _tipoComida = MutableStateFlow("")
    val tipoComida: StateFlow<String> = _tipoComida.asStateFlow()

    // ── Guardar ───────────────────────────────────────────────────────────────
    private val _saveState = MutableStateFlow<SaveUiState>(SaveUiState.Idle)
    val saveState: StateFlow<SaveUiState> = _saveState.asStateFlow()

    init {
        // Debounce: espera 500ms después de que el usuario deja de escribir
        viewModelScope.launch {
            _query
                .debounce(500L)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collect { q -> buscarAlimentos(q) }
        }
    }

    fun onQueryChange(q: String) { _query.value = q }

    fun onTipoComidaChange(tipo: String) { _tipoComida.value = tipo }

    // ── Agregar al carrito ────────────────────────────────────────────────────
    fun addToCart(alimento: Alimento) {
        val current = _cart.value.toMutableList()
        val existing = current.indexOfFirst { it.alimento.fdcId == alimento.fdcId }
        if (existing >= 0) {
            // Si ya está, suma 100g más
            val item = current[existing]
            current[existing] = item.copy(gramos = item.gramos + 100)
        } else {
            current.add(CartItem(alimento))
        }
        _cart.value = current
    }

    fun removeFromCart(fdcId: String) {
        _cart.value = _cart.value.filter { it.alimento.fdcId != fdcId }
    }

    fun updateGramos(fdcId: String, gramos: Double) {
        _cart.value = _cart.value.map {
            if (it.alimento.fdcId == fdcId) it.copy(gramos = gramos) else it
        }
    }

    // ── Totales del carrito ───────────────────────────────────────────────────
    val totalKcal:     Double get() = _cart.value.sumOf { it.kcal }
    val totalProteinas:Double get() = _cart.value.sumOf { it.proteinas }
    val totalCarbos:   Double get() = _cart.value.sumOf { it.carbos }
    val totalGrasas:   Double get() = _cart.value.sumOf { it.grasas }

    // ── Búsqueda en USDA ──────────────────────────────────────────────────────
    private fun buscarAlimentos(query: String) {
        viewModelScope.launch {
            _searchState.value = SearchUiState.Loading
            try {
                val alimentos = searchAlimentoUseCase(query)
                _searchState.value = SearchUiState.Success(alimentos)
            } catch (e: Exception) {
                _searchState.value = SearchUiState.Error(e.message ?: "Error de búsqueda")
            }
        }
    }

    // ── Guardar comida en Firestore ───────────────────────────────────────────
    fun confirmarComida(onSuccess: () -> Unit) {
        if (_cart.value.isEmpty() || _tipoComida.value.isBlank()) return

        viewModelScope.launch {
            _saveState.value = SaveUiState.Loading
            try {
                val uid = authRepository.getCurrentUserId()
                    ?: throw Exception("Sin usuario")

                val nombres = _cart.value.joinToString(", ") { it.alimento.nombre }

                val comida = Comida(
                    id            = "",
                    userId        = uid,
                    nombre        = nombres,
                    tipo          = _tipoComida.value,
                    totalKcal     = totalKcal,
                    proteinas     = totalProteinas,
                    carbohidratos = totalCarbos,
                    grasas        = totalGrasas,
                    timestamp     = java.util.Date(),
                    fecha         = java.text.SimpleDateFormat(
                        "yyyy-MM-dd", java.util.Locale.getDefault()
                    ).format(java.util.Date()),
                    url           = ""
                )

                alimentoRepository.saveComida(uid, comida).getOrThrow()

                _saveState.value = SaveUiState.Success
                _cart.value      = emptyList()
                _query.value     = ""
                _tipoComida.value = ""
                onSuccess()

            } catch (e: Exception) {
                _saveState.value = SaveUiState.Error(e.message ?: "Error al guardar")
            }
        }
    }

    fun resetSaveState() { _saveState.value = SaveUiState.Idle }
}
