package com.example.nutrimetrix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutrimetrix.core.navigation.NavGraph
import com.example.nutrimetrix.ui.splash.SplashScreen
import com.example.nutrimetrix.ui.splash.SplashViewModel
import com.example.nutrimetrix.ui.theme.NutriMetrixTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // La splash nativa del sistema se muestra aquí (ícono sobre fondo blanco/negro)
        // Se cierra sola cuando setContent termina de componerse — no la forzamos
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            NutriMetrixTheme {

                val startDestination by splashViewModel.startDestination
                    .collectAsStateWithLifecycle()

                // showSplash controla si muestro la pantalla verde o la app real
                var showSplash by remember { mutableStateOf(true) }

                when {
                    // Mientras la animación de SplashScreen no terminó → mostrar splash verde
                    showSplash -> {
                        SplashScreen(
                            onAnimationFinish = { showSplash = false }
                        )
                    }
                    // Animación terminada + destino listo → entrar a la app
                    startDestination != null -> {
                        NavGraph(startDestination = startDestination!!)
                    }
                    // Animación terminada pero Firebase todavía no respondió (raro, <100ms)
                    // simplemente espero sin mostrar nada
                }
            }
        }
    }
}
