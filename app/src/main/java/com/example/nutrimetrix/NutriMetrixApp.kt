package com.example.nutrimetrix

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class requerida por Hilt.
 * Hay que declarar esta clase en el AndroidManifest:
 *
 *   <application
 *       android:name=".NutriMetrixApp"
 *       ...
 */
@HiltAndroidApp
class NutriMetrixApp : Application()
