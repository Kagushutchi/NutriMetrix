import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.example.nutrimetrix"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nutrimetrix"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        val localProps = Properties()
        val localFile  = rootProject.file("local.properties")
        if (localFile.exists()) localProps.load(localFile.inputStream())

        buildConfigField(
            "String",
            "USDA_API_KEY",
            "\"${localProps.getProperty("USDA_API_KEY", "")}\""
        )

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProps.getProperty("GEMINI_API_KEY", "")}\""
        )
    }



    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {

    // ── Jetpack Compose ──────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // collectAsStateWithLifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)

    // ── Navegación ───────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── Hilt ─────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ── Splash API ───────────────────────────────────────────────
    implementation(libs.androidx.core.splashscreen)

    // ── Room ─────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Retrofit + OkHttp ────────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // ── Firebase ─────────────────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    // ── Glide ────────────────────────────────────────────────────
    implementation(libs.glide)

    // ── Coroutines ───────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── Debug ────────────────────────────────────────────────────
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // ── Testing ──────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.material.icons.extended)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
// CameraX
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

// Coil — para mostrar la foto capturada
    implementation("io.coil-kt:coil-compose:2.6.0")

// Accompanist — permiso de cámara con Compose
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("com.google.guava:guava:32.1.3-android")
// Firebase Storage — para guardar la imagen
    implementation("com.google.firebase:firebase-storage-ktx")
}
