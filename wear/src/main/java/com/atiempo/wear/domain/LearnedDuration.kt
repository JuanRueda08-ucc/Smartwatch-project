package com.atiempo.wear.domain

import kotlin.math.roundToInt

/**
 * Base aprendida de un paso (MODELO_ESTIMACION.md, paso 1): media móvil exponencial de las duraciones
 * reales, del registro más antiguo al más reciente, con α = 0.3 (lo reciente pesa más).
 * La usa la pantalla de ajustes y la usará el DurationEstimator (Fase 4).
 */
object LearnedDuration {
    const val ALPHA = 0.3

    /** EMA de `minutes` en orden cronológico; null si no hay registros. */
    fun ema(minutes: List<Int>, alpha: Double = ALPHA): Double? {
        if (minutes.isEmpty()) return null
        require(alpha in 0.0..1.0) { "α fuera de rango: $alpha" }
        return minutes.drop(1).fold(minutes.first().toDouble()) { acc, value -> alpha * value + (1 - alpha) * acc }
    }

    /** La EMA redondeada a minutos enteros, o el valor por defecto si no hay historial. */
    fun minutesOr(default: Int, minutes: List<Int>): Int = ema(minutes)?.roundToInt() ?: default
}
