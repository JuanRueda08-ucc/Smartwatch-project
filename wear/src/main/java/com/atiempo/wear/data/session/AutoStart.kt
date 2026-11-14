package com.atiempo.wear.data.session

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.data.repo.planFor

/** Lógica pura: qué evento debe empezar su preparación sola ahora. */
object AutoStart {

    /**
     * El evento más próximo cuyo primer aviso ya llegó y que todavía no empieza, sin contar los que ya
     * tienen sesión (`excluded`). null si ninguno.
     */
    fun eventToStart(events: List<Event>, routines: List<Routine>, now: Long, excluded: Set<Long>): Event? {
        val routineById = routines.associateBy { it.id }
        return events
            .filter { it.id !in excluded && it.startAt > now }
            .filter { event -> routineById[event.routineId]?.planFor(event)?.let { now >= it.finishAt } == true }
            .minByOrNull { it.startAt }
    }

    /**
     * Hora del próximo primer aviso entre los eventos sin sesión (para la alarma cuando no hay preparación
     * en curso). null si no hay ninguno por venir.
     */
    fun nextFirstAlertAt(events: List<Event>, routines: List<Routine>, now: Long, excluded: Set<Long>): Long? {
        val routineById = routines.associateBy { it.id }
        return events
            .filter { it.id !in excluded && it.startAt > now }
            .mapNotNull { event -> routineById[event.routineId]?.planFor(event)?.finishAt }
            .filter { it > now }
            .minOrNull()
    }
}
