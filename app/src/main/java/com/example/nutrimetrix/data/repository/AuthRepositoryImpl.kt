package com.example.nutrimetrix.data.repository

import android.content.Context
import com.example.nutrimetrix.domain.model.User
import com.example.nutrimetrix.domain.repository.IAuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore:    FirebaseFirestore,
    @ApplicationContext private val context: Context
) : IAuthRepository {

    override suspend fun signInWithGoogle(idToken: String): Result<Boolean> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("No UID found"))

            // Verificar si el usuario ya tiene perfil registrado en Firestore
            val doc = firestore.collection("usuarios").document(uid).get().await()
            val isNewUser = !doc.exists()
            Result.success(isNewUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            val userMap = mapOf(
                "id"               to user.id,
                "mail"             to user.mail,
                "peso"             to user.peso,
                "altura"           to user.altura,
                "edad"             to user.edad,
                "genero"           to user.genero,
                "objetivo"         to user.objetivo,
                "nivel_actividad"  to user.nivelActividad,
                "peso_ideal"       to user.pesoIdeal,
                "calorias_diarias" to user.caloriasDiarias,
                "proteinas"        to user.proteinas,
                "carbohidratos"    to user.carbohidratos,
                "grasas"           to user.grasas
            )
            firestore.collection("usuarios").document(user.id).set(userMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(userId: String): User? {
        return try {
            val doc = firestore.collection("usuarios").document(userId).get().await()
            if (doc.exists()) {
                User(
                    id             = doc.getString("id") ?: userId,
                    mail           = doc.getString("mail") ?: "",
                    peso           = doc.getDouble("peso") ?: 0.0,
                    altura         = doc.getLong("altura")?.toInt() ?: 0,
                    edad           = doc.getLong("edad")?.toInt() ?: 0,
                    genero         = doc.getString("genero") ?: "",
                    objetivo       = doc.getString("objetivo") ?: "",
                    nivelActividad = doc.getString("nivel_actividad") ?: "",
                    pesoIdeal      = doc.getDouble("peso_ideal") ?: 0.0,
                    caloriasDiarias = doc.getLong("calorias_diarias")?.toInt() ?: 0,
                    proteinas      = doc.getDouble("proteinas") ?: 0.0,
                    carbohidratos  = doc.getDouble("carbohidratos") ?: 0.0,
                    grasas         = doc.getDouble("grasas") ?: 0.0
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    override fun getCurrentUserName(): String? {
        return firebaseAuth.currentUser?.displayName
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            
            // Sign out Google Client
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            googleSignInClient.signOut().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
