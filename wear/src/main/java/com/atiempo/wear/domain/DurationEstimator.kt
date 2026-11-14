package com.atiempo.wear.domain

import com.atiempo.wear.domain.model.Adjustment
import com.atiempo.wear.domain.model.AdjustmentReason
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.domain.model.TrafficLevel
import java.time.DayOfWeek
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Un registro del historial: cuánto tardó de verdad un paso y qué día de la semana (ISO). */
data class StepHistory(val actualMin: Int, val dayOfWeek: Int)

/** Contexto del día (simulado en esta entrega, D-003). */
data class EstimationContext(
    val rain: Boolean = false,
    val traffic: TrafficLevel = TrafficLevel.LOW,
    /** Destino, para la frase "Tráfico alto hacia…". */
    val place: String? = null,
)

/** Lo que se sabe de un paso: su valor por defecto, el ajuste manual y su historial cronológico. */
data class StepInputs(val defaultMin: Int, val manualMin: Int?, val history: List<StepHistory>)

/** Minutos de cada paso y por qué (sin el margen de seguridad, que suma `PrepPlanner`). */
data class Estimate(val durations: StepDurations, val adjustments: List<Adjustment>)

/**
 * Modelo de estimación de MODELO_ESTIMACION.md. Simple y explicable: cada término distinto de cero
 * genera una razón con su efecto en minutos respecto al valor por defecto de la rutina.
 *
 * minutosPaso = (manual ?: EMA ?: defecto) + efectoDía + efectoContexto
 */
object DurationEstimator {
    const val MIN_DAY_RECORDS = 3
    const val MAX_DAY_EFFECT = 10
    private const val RAIN_RATIO = 0.15
    private const val RAIN_MIN = 2
    private const val TRAFFIC_MEDIUM = 0.10
    private const val TRAFFIC_HIGH = 0.25

    fun estimate(steps: Map<PrepStep, StepInputs>, day: DayOfWeek, context: EstimationContext = EstimationContext()): Estimate {
        val adjustments = mutableListOf<Adjustment>()
        val minutes = PrepStep.entries.associateWith { step ->
            val inputs = steps[step] ?: StepInputs(defaultMin = 0, manualMin = null, history = emptyList())
            estimateStep(step, inputs, day, context, adjustments)
        }
        return Estimate(
            durations = StepDurations(
                finish = minutes.getValue(PrepStep.FINISH),
                prepare = minutes.getValue(PrepStep.PREPARE),
                check = minutes.getValue(PrepStep.CHECK),
                travel = minutes.getValue(PrepStep.LEAVE),
            ),
            adjustments = adjustments.sortedByDescending { it.minutes },
        )
    }

    private fun estimateStep(
        step: PrepStep,
        inputs: StepInputs,
        day: DayOfWeek,
        context: EstimationContext,
        adjustments: MutableList<Adjustment>,
    ): Int {
        val ema = LearnedDuration.ema(inputs.history.map { it.actualMin })
        val learned = ema?.roundToInt()

        // 1. Base: el ajuste manual reemplaza lo aprendido; sin historial, el valor por defecto.
        var minutes = inputs.manualMin ?: learned ?: inputs.defaultMin
        if (inputs.manualMin != null) {
            val delta = inputs.manualMin - (learned ?: inputs.defaultMin)
            if (delta != 0) adjustments += Adjustment(AdjustmentReason.Manual(step, inputs.manualMin), delta)
        } else if (learned != null && learned != inputs.defaultMin) {
            adjustments += Adjustment(AdjustmentReason.Learned(step, learned), learned - inputs.defaultMin)
        }

        // 2. Efecto del día: solo con 3 o más registros de ese día, limitado a ±10 min.
        val sameDay = inputs.history.filter { it.dayOfWeek == day.value }
        if (ema != null && sameDay.size >= MIN_DAY_RECORDS) {
            val effect = (sameDay.map { it.actualMin }.average() - ema).roundToInt().coerceIn(-MAX_DAY_EFFECT, MAX_DAY_EFFECT)
            if (effect != 0) {
                minutes += effect
                adjustments += Adjustment(AdjustmentReason.DayEffect(day, step), effect)
            }
        }

        // 3. Contexto: solo el trayecto.
        if (step == PrepStep.LEAVE) {
            val travel = minutes
            if (context.rain) {
                val extra = maxOf(RAIN_MIN, ceilMinutes(travel * RAIN_RATIO))
                minutes += extra
                adjustments += Adjustment(AdjustmentReason.Rain, extra)
            }
            val trafficRatio = when (context.traffic) {
                TrafficLevel.LOW -> 0.0
                TrafficLevel.MEDIUM -> TRAFFIC_MEDIUM
                TrafficLevel.HIGH -> TRAFFIC_HIGH
            }
            if (trafficRatio > 0) {
                val extra = ceilMinutes(travel * trafficRatio)
                minutes += extra
                adjustments += Adjustment(AdjustmentReason.Traffic(context.traffic, context.place), extra)
            }
        }
        return minutes.coerceAtLeast(1)
    }

    private fun ceilMinutes(value: Double): Int = ceil(value - 1e-9).toInt()
}
