package com.example.nutrimetrix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NutriColorScheme = lightColorScheme(
    primary          = GreenPrimary,
    onPrimary        = White,
    background       = BackgroundLight,
    surface          = SurfaceWhite,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
)

@Composable
fun NutriMetrixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NutriColorScheme,
        typography  = Typography,
        content     = content
    )
}
