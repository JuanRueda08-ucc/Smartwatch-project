package com.atiempo.wear.data.repo

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.data.db.StepRecord
import com.atiempo.wear.domain.StepHistory
import java.time.ZoneId

/** Agrupa el historial por rutina y paso (del más antiguo al más reciente) para el DurationEstimator. */
fun List<StepRecord>.toEstimationData(zone: ZoneId): EstimationData = EstimationData(
    history = groupBy { it.routineId }.mapValues { (_, byRoutine) ->
        byRoutine.groupBy { it.step }.mapValues { (_, list) -> list.map { StepHistory(it.actualMin, it.dayOfWeek) } }
    },
    zone = zone,
)

/**
 * Cuánto se movió el primer aviso por lo aprendido en una preparación (tarea 4.4), en minutos:
 * positivo = la próxima vez aviso antes; negativo = después; 0 = la rutina sigue igual.
 * Compara el plan de ese mismo evento con el historial sin y con los registros de la sesión, sin clima ni
 * tráfico, para que solo cuente lo aprendido. Un ajuste manual del paso hace que lo aprendido no lo mueva.
 */
fun Routine.nextTimeShift(event: Event, records: List<StepRecord>, sessionId: Long, zone: ZoneId): Int {
    val before = planFor(event, records.filter { it.sessionId != sessionId }.toEstimationData(zone))
    val after = planFor(event, records.toEstimationData(zone))
    return ((before.finishAt - after.finishAt) / MINUTE_MS).toInt()
}

private const val MINUTE_MS = 60_000L
