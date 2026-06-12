package com.example.nutrimetrix.domain.model

import kotlin.math.abs

/**
 * Representación de un usuario en el dominio.
 */
data class User(
    val id:             String,
    val mail:           String,
    val peso:           Double,
    val altura:         Int,
    val edad:           Int,
    val genero:         String,
    val objetivo:       String,
    val nivelActividad: String,
    val pesoIdeal:      Double,
    val caloriasDiarias:Int,
    val proteinas:      Double,
    val carbohidratos:  Double,
    val grasas:         Double
) {
    companion object {
        /**
         * Calcula el resultado nutricional (calorías y macronutrientes)
         * basado en los parámetros corporales y el objetivo del usuario.
         */
        fun calcularNutricion(
            peso: Double,
            altura: Int,
            edad: Int,
            genero: String,
            nivelActividad: String,
            objetivo: String,
            pesoIdeal: Double
        ): NutritionResult {
            // Tasa Metabólica Basal (TMB) usando Harris-Benedict revisado
            val tmb = if (genero.uppercase() == "MASCULINO") {
                (10 * peso) + (6.25 * altura) - (5 * edad) + 5
            } else {
                (10 * peso) + (6.25 * altura) - (5 * edad) - 161
            }

            // Factor de actividad física
            val factor = when (nivelActividad.uppercase()) {
                "SEDENTARIO" -> 1.2
                "LIGERO"     -> 1.375
                "MODERADO"   -> 1.55
                "ACTIVO"     -> 1.725
                "MUY_ACTIVO" -> 1.9
                else         -> 1.2
            }
            val get = tmb * factor

            // Ajuste calórico según objetivo
            val caloriasObjetivo = when (objetivo.uppercase()) {
                "DEFICIT"   -> get - 500
                "SUPERAVIT" -> get + 400
                else        -> get
            }

            // Distribución de macronutrientes
            val proteinas = peso * 2.0
            val grasas    = (caloriasObjetivo * 0.25) / 9.0
            val carbos    = (caloriasObjetivo - (proteinas * 4) - (grasas * 9)) / 4.0

            // Cálculo de semanas estimadas para alcanzar el peso ideal (0.5 kg por semana)
            val diferencia = abs(peso - pesoIdeal)
            val semanasEstimadas = if (diferencia < 1) {
                "Ya estás en tu peso ideal"
            } else {
                "${(diferencia / 0.5).toInt()} semanas aprox."
            }

            return NutritionResult(
                caloriasObjetivo = caloriasObjetivo.toInt(),
                proteinas        = proteinas,
                carbohidratos    = carbos,
                grasas           = grasas,
                semanasEstimadas = semanasEstimadas
            )
        }
    }
}

/**
 * Resultado de los cálculos nutricionales y metabólicos de un usuario.
 */
data class NutritionResult(
    val caloriasObjetivo: Int,
    val proteinas: Double,
    val carbohidratos: Double,
    val grasas: Double,
    val semanasEstimadas: String
)
