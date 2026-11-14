package com.atiempo.wear.domain

import com.atiempo.wear.domain.model.PrepStep
import kotlin.math.ceil

/** Minutos de cada paso. `travel` es el desplazamiento: el paso LEAVE termina al llegar. */
data class StepDurations(val finish: Int, val prepare: Int, val check: Int, val travel: Int) {
    val total: Int get() = finish + prepare + check + travel
}

/**
 * Plan de un evento: hora de inicio de cada estado, en epoch ms.
 * `leaveAt` es la hora de salir; `arriveAt` la llegada estimada (antes del evento por el margen).
 */
data class PrepPlan(
    val finishAt: Long,
    val prepareAt: Long,
    val checkAt: Long,
    val leaveAt: Long,
    val arriveAt: Long,
    val eventAt: Long,
    val marginMin: Int,
) {
    fun startOf(step: PrepStep): Long = when (step) {
        PrepStep.FINISH -> finishAt
        PrepStep.PREPARE -> prepareAt
        PrepStep.CHECK -> checkAt
        PrepStep.LEAVE -> leaveAt
    }
}

/**
 * Calcula el plan hacia atrás desde la hora del evento (MODELO_ESTIMACION.md):
 * inicioPaso4 = horaEvento − desplazamiento − margen, y cada paso anterior resta su duración.
 * Las duraciones las decide quien llama: hoy, la rutina; en la Fase 4, el DurationEstimator.
 */
object PrepPlanner {
    const val MIN_MARGIN = 3
    private const val MARGIN_RATIO = 0.10
    private const val MINUTE = 60_000L

    /** Margen de seguridad: 10 % del total, redondeado hacia arriba, mínimo 3 min. */
    fun margin(durations: StepDurations): Int =
        maxOf(MIN_MARGIN, ceil(durations.total * MARGIN_RATIO - 1e-9).toInt())

    fun plan(eventAt: Long, durations: StepDurations): PrepPlan {
        require(listOf(durations.finish, durations.prepare, durations.check, durations.travel).all { it >= 0 }) {
            "Duraciones negativas: $durations"
        }
        val margin = margin(durations)
        val leaveAt = eventAt - (durations.travel + margin) * MINUTE
        val checkAt = leaveAt - durations.check * MINUTE
        val prepareAt = checkAt - durations.prepare * MINUTE
        val finishAt = prepareAt - durations.finish * MINUTE
        return PrepPlan(
            finishAt = finishAt,
            prepareAt = prepareAt,
            checkAt = checkAt,
            leaveAt = leaveAt,
            arriveAt = leaveAt + durations.travel * MINUTE,
            eventAt = eventAt,
            marginMin = margin,
        )
    }
}
