package com.atiempo.wear.data.repo

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.domain.PrepPlan
import com.atiempo.wear.domain.PrepPlanner
import com.atiempo.wear.domain.StepDurations

/**
 * Duraciones de cada paso para un evento: el ajuste manual de la rutina si existe, si no su valor por
 * defecto; el desplazamiento sale del evento. En la Fase 4 esto pasa por el DurationEstimator.
 */
fun Routine.stepDurations(event: Event) = StepDurations(
    finish = manualFinishMin ?: defaultFinishMin,
    prepare = manualPrepareMin ?: defaultPrepareMin,
    check = manualCheckMin ?: defaultCheckMin,
    travel = manualTravelMin ?: event.baseTravelMin,
)

fun Routine.planFor(event: Event): PrepPlan = PrepPlanner.plan(event.startAt, stepDurations(event))
