package com.atiempo.wear.data.repo

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.domain.PrepPlanner
import com.atiempo.wear.domain.model.Adjustment
import com.atiempo.wear.domain.model.AdjustmentReason
import com.atiempo.wear.domain.model.PrepStep

/**
 * Razones de la hora de los avisos de un evento, de mayor a menor efecto (MODELO_ESTIMACION.md).
 *
 * Provisional hasta la Fase 4: hoy solo existen los ajustes manuales de la rutina y el margen de
 * seguridad. El DurationEstimator sumará la base aprendida, el efecto del día, la lluvia y el tráfico
 * con el mismo modelo (D-025).
 */
fun Routine.explain(event: Event): List<Adjustment> {
    val manual = listOf(
        Triple(PrepStep.FINISH, manualFinishMin, defaultFinishMin),
        Triple(PrepStep.PREPARE, manualPrepareMin, defaultPrepareMin),
        Triple(PrepStep.CHECK, manualCheckMin, defaultCheckMin),
        Triple(PrepStep.LEAVE, manualTravelMin, event.baseTravelMin),
    ).mapNotNull { (step, chosen, base) ->
        if (chosen == null || chosen == base) null else Adjustment(AdjustmentReason.Manual(step, chosen), chosen - base)
    }
    val margin = Adjustment(AdjustmentReason.SafetyMargin, PrepPlanner.margin(stepDurations(event)))
    return (manual + margin).sortedByDescending { it.minutes }
}
