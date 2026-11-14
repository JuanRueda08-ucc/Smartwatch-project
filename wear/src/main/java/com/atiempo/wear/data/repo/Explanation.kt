package com.atiempo.wear.data.repo

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.domain.PrepPlanner
import com.atiempo.wear.domain.model.Adjustment
import com.atiempo.wear.domain.model.AdjustmentReason

/**
 * Razones de la hora de los avisos de un evento, de mayor a menor efecto (MODELO_ESTIMACION.md): las del
 * DurationEstimator (lo aprendido, el día, el contexto y los ajustes manuales) más el margen de seguridad.
 */
fun Routine.explain(event: Event, data: EstimationData = EstimationData.NONE): List<Adjustment> {
    val estimate = estimate(event, data)
    val margin = Adjustment(AdjustmentReason.SafetyMargin, PrepPlanner.margin(estimate.durations))
    return (estimate.adjustments + margin).sortedByDescending { it.minutes }
}
