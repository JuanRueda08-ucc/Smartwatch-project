package com.atiempo.wear.data.repo

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.domain.DurationEstimator
import com.atiempo.wear.domain.Estimate
import com.atiempo.wear.domain.EstimationContext
import com.atiempo.wear.domain.PrepPlan
import com.atiempo.wear.domain.PrepPlanner
import com.atiempo.wear.domain.StepDurations
import com.atiempo.wear.domain.StepHistory
import com.atiempo.wear.domain.StepInputs
import com.atiempo.wear.domain.model.PrepStep
import java.time.Instant
import java.time.ZoneId

/**
 * Lo que el estimador necesita además de la rutina y el evento: el historial de cada rutina y paso
 * (del más antiguo al más reciente) y el contexto del día.
 */
data class EstimationData(
    val history: Map<Long, Map<PrepStep, List<StepHistory>>> = emptyMap(),
    val context: EstimationContext = EstimationContext(),
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    companion object {
        /** Sin historial ni contexto: el estimador usa el ajuste manual o el valor por defecto. */
        val NONE = EstimationData()
    }
}

/** Estimación de cada paso para un evento con el DurationEstimator (tarea 4.1). */
fun Routine.estimate(event: Event, data: EstimationData = EstimationData.NONE): Estimate {
    val inputs = PrepStep.entries.associateWith { step ->
        StepInputs(
            // El trayecto por defecto es el del evento (depende del lugar), no el de la rutina.
            defaultMin = if (step == PrepStep.LEAVE) event.baseTravelMin else defaultFor(step),
            manualMin = manualFor(step),
            history = data.history[id]?.get(step).orEmpty(),
        )
    }
    val day = Instant.ofEpochMilli(event.startAt).atZone(data.zone).dayOfWeek
    return DurationEstimator.estimate(inputs, day, data.context.copy(place = event.place))
}

fun Routine.stepDurations(event: Event, data: EstimationData = EstimationData.NONE): StepDurations =
    estimate(event, data).durations

fun Routine.planFor(event: Event, data: EstimationData = EstimationData.NONE): PrepPlan =
    PrepPlanner.plan(event.startAt, stepDurations(event, data))
