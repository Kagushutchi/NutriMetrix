package com.example.nutrimetrix.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
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
    private val firebaseAuth: FirebaseAuth,
    private val firestore:    FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init { cargarGaleria() }

    fun cargarGaleria() {
        viewModelScope.launch {
            _uiState.value = GalleryUiState.Loading
            try {
                val uid = firebaseAuth.currentUser?.uid
                    ?: throw Exception("Usuario no autenticado")

                // Traemos todas las comidas ordenadas por timestamp descendente
                val snapshot = firestore
                    .collection("usuarios")
                    .document(uid)
                    .collection("comidas")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val comidas = snapshot.documents.mapNotNull { doc ->
                    val timestamp = doc.getTimestamp("timestamp") ?: return@mapNotNull null

                    ComidaGaleria(
                        id        = doc.id,
                        tipo      = doc.getString("tipo")     ?: "",
                        nombre    = doc.getString("nombre")   ?: "",
                        totalKcal = doc.getDouble("totalKcal")     ?: 0.0,
                        proteinas = doc.getDouble("proteinas")     ?: 0.0,
                        carbos    = doc.getDouble("carbohidratos") ?: 0.0,
                        grasas    = doc.getDouble("grasas")        ?: 0.0,
                        fecha     = formatFecha(timestamp),
                        hora      = formatHora(timestamp),
                        imageUrl  = doc.getString("url") ?: ""
                    )
                }

                _uiState.value = GalleryUiState.Success(comidas)

            } catch (e: Exception) {
                _uiState.value = GalleryUiState.Error(e.message ?: "Error al cargar galería")
            }
        }
    }

    private fun formatFecha(ts: Timestamp): String {
        val sdf = SimpleDateFormat("d MMMM", Locale("es"))
        return sdf.format(ts.toDate())
    }

    private fun formatHora(ts: Timestamp): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(ts.toDate())
    }
}
