package com.example.nutrimetrix.ui.food.detail

import androidx.lifecycle.SavedStateHandle
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
    private val firebaseAuth: FirebaseAuth,
    private val firestore:    FirebaseFirestore,
    savedStateHandle:         SavedStateHandle   // lee el foodId de la ruta
) : ViewModel() {

    private val _uiState = MutableStateFlow<FoodDetailUiState>(FoodDetailUiState.Loading)
    val uiState: StateFlow<FoodDetailUiState> = _uiState.asStateFlow()

    // foodId viene del argumento de navegación "food_detail/{foodId}"
    private val foodId: String = checkNotNull(savedStateHandle["foodId"])

    init { cargarDetalle() }

    private fun cargarDetalle() {
        viewModelScope.launch {
            try {
                val uid = firebaseAuth.currentUser?.uid
                    ?: throw Exception("Usuario no autenticado")

                val doc = firestore
                    .collection("usuarios")
                    .document(uid)
                    .collection("comidas")
                    .document(foodId)
                    .get()
                    .await()

                if (!doc.exists()) throw Exception("Comida no encontrada")

                val timestamp = doc.getTimestamp("timestamp")
                val fecha = timestamp?.let {
                    java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("es"))
                        .format(it.toDate())
                } ?: ""
                val hora = timestamp?.let {
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(it.toDate())
                } ?: ""

                _uiState.value = FoodDetailUiState.Success(
                    ComidaDetalle(
                        id        = doc.id,
                        tipo      = doc.getString("tipo")     ?: "",
                        nombre    = doc.getString("nombre")   ?: "",
                        totalKcal = doc.getDouble("totalKcal")     ?: 0.0,
                        proteinas = doc.getDouble("proteinas")     ?: 0.0,
                        carbos    = doc.getDouble("carbohidratos") ?: 0.0,
                        grasas    = doc.getDouble("grasas")        ?: 0.0,
                        fecha     = fecha,
                        hora      = hora,
                        imageUrl  = doc.getString("url") ?: ""
                    )
                )
            } catch (e: Exception) {
                _uiState.value = FoodDetailUiState.Error(e.message ?: "Error al cargar detalle")
            }
        }
    }
}
