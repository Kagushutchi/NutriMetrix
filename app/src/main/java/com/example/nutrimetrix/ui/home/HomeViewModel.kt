package com.example.nutrimetrix.ui.home

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
    private val firebaseAuth: FirebaseAuth,
    private val firestore:    FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { cargarDatosHome() }

    fun cargarDatosHome() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val user = firebaseAuth.currentUser
                    ?: throw Exception("Usuario no autenticado")

                val firstName = user.displayName?.split(" ")?.firstOrNull() ?: "Usuario"
                val uid       = user.uid

                val userDoc = firestore.collection("usuarios").document(uid).get().await()

                val caloriasObjetivo  = (userDoc.getLong("calorias_diarias") ?: 0L).toInt()
                val proteinasObjetivo = userDoc.getDouble("proteinas")      ?: 0.0
                val carbosObjetivo    = userDoc.getDouble("carbohidratos")   ?: 0.0
                val grasasObjetivo    = userDoc.getDouble("grasas")          ?: 0.0

                val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())

                // AQUÍ EL CAMBIO MAGNÍFICO: Usar SnapshotListener para tiempo real
                firestore
                    .collection("usuarios").document(uid)
                    .collection("comidas")
                    .whereGreaterThanOrEqualTo("fecha", hoy)
                    .addSnapshotListener { snapshot, error ->

                        if (error != null) {
                            _uiState.value = HomeUiState.Error(error.message ?: "Error al escuchar datos")
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            var caloriasConsumidas  = 0
                            var proteinasConsumidas = 0.0
                            var carbosConsumidos    = 0.0
                            var grasasConsumidas    = 0.0
                            val listaComidas        = mutableListOf<ComidaResumen>()

                            for (doc in snapshot.documents) {
                                // CORRECCIÓN DE CASTEO: Usamos getDouble para "totalKcal" porque así lo guardas en los ViewModels
                                val kcal      = doc.getDouble("totalKcal")?.toInt() ?: 0
                                val proteinas = doc.getDouble("proteinas")     ?: 0.0
                                val carbos    = doc.getDouble("carbohidratos")  ?: 0.0
                                val grasas    = doc.getDouble("grasas")         ?: 0.0
                                val nombre    = doc.getString("nombre")         ?: ""
                                val tipo      = doc.getString("tipo")           ?: ""

                                caloriasConsumidas  += kcal
                                proteinasConsumidas += proteinas
                                carbosConsumidos    += carbos
                                grasasConsumidas    += grasas

                                listaComidas.add(
                                    ComidaResumen(
                                        id        = doc.id,
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
                    }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error al inicializar datos")
            }
        }
    }
}
