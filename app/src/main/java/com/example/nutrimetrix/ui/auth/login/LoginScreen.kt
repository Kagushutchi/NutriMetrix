package com.example.nutrimetrix.ui.auth.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutrimetrix.R
import com.example.nutrimetrix.ui.auth.AuthUiState
import com.example.nutrimetrix.ui.auth.AuthViewModel
import com.example.nutrimetrix.ui.theme.GreenPrimary
import com.example.nutrimetrix.ui.theme.NutriMetrixTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    onLoginSuccess: (isNewUser: Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val googleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                account.idToken?.let { viewModel.signInWithGoogle(it) }
            } catch (e: ApiException) {
            }
        }
    }

    // Reacciona al estado — ahora Success lleva isNewUser dentro
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            val isNewUser = (uiState as AuthUiState.Success).isNewUser
            viewModel.resetState()
            onLoginSuccess(isNewUser)
        }
    }

    LoginContent(
        isLoading = uiState is AuthUiState.Loading,
        error     = (uiState as? AuthUiState.Error)?.message,
        onGoogleClick = {
            // Forzamos el selector de cuenta para que siempre muestre el picker
            googleSignInClient.signOut().addOnCompleteListener {
                launcher.launch(googleSignInClient.signInIntent)
            }
        }
    )
}

@Composable
private fun LoginContent(
    isLoading: Boolean,
    error: String?,
    onGoogleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "NutriMetrix Logo",
            modifier = Modifier.size(90.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "NUTRIMETRIX",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(48.dp))

        Text("Bienvenido", fontSize = 32.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Inicia Sesión en NutriMetrix",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(60.dp))

        if (isLoading) {
            CircularProgressIndicator(color = GreenPrimary)
        } else {
            Button(
                onClick = onGoogleClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text(
                    text = "Continuar con Google",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(text = it, color = Color.Red, fontSize = 13.sp)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    NutriMetrixTheme {
        LoginContent(isLoading = false, error = null, onGoogleClick = {})
    }
}
