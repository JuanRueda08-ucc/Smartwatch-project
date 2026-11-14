package com.atiempo.wear.data.repo

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.data.seed.SampleData
import com.atiempo.wear.domain.model.ParsedEvent
import com.atiempo.wear.domain.model.RoutineKind
import java.time.LocalDateTime
import java.time.ZoneId

/** Cada tipo de rutina del intérprete corresponde a una de las 3 rutinas sembradas (D-007). */
fun RoutineKind.routineId(): Long = when (this) {
    RoutineKind.CLASS -> SampleData.CLASE_ID
    RoutineKind.APPOINTMENT -> SampleData.CITA_ID
    RoutineKind.SOCIAL -> SampleData.SOCIAL_ID
}

/** Hora de inicio del evento interpretado, epoch ms. */
fun ParsedEvent.startAt(zone: ZoneId): Long =
    LocalDateTime.of(date.value, time.value).atZone(zone).toInstant().toEpochMilli()

/** Un evento no se puede guardar si su hora ya pasó. */
fun ParsedEvent.isPast(now: Long, zone: ZoneId): Boolean = startAt(zone) <= now

/**
 * Evento listo para guardar. El trayecto base es el de la rutina (ajuste manual o valor por defecto),
 * porque la frase no lo dice. null si la rutina no existe.
 */
fun ParsedEvent.toEvent(routines: List<Routine>, zone: ZoneId): Event? {
    val routine = routines.firstOrNull { it.id == routine.value.routineId() } ?: return null
    return Event(
        title = title.value,
        startAt = startAt(zone),
        routineId = routine.id,
        place = place.value,
        baseTravelMin = routine.manualTravelMin ?: routine.defaultTravelMin,
        createdByVoice = true,
    )
}
