package com.atiempo.wear.data.repo

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.EventDao
import com.atiempo.wear.data.db.PrepSession
import com.atiempo.wear.data.db.PrepSessionDao
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.data.db.RoutineDao
import com.atiempo.wear.data.db.StepRecord
import com.atiempo.wear.data.db.StepRecordDao
import com.atiempo.wear.domain.model.PrepStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/*
 * Repositorios delgados: por ahora solo envuelven los DAOs. En la Fase 3 sumarán la lógica que
 * combina datos (por ejemplo, el plan de un evento con su rutina y su historial).
 */

class EventRepository(private val events: EventDao) {
    fun upcoming(from: Long): Flow<List<Event>> = events.observeUpcoming(from)
    fun event(id: Long): Flow<Event?> = events.observe(id)
    suspend fun add(event: Event): Long = events.insert(event)
    suspend fun get(id: Long): Event? = events.get(id)
    suspend fun update(event: Event) = events.update(event)
}

class RoutineRepository(private val routines: RoutineDao) {
    fun all(): Flow<List<Routine>> = routines.observeAll()
    fun routine(id: Long): Flow<Routine?> = routines.observe(id)
    suspend fun get(id: Long): Routine? = routines.get(id)
    suspend fun update(routine: Routine) = routines.update(routine)
}

class HistoryRepository(private val sessions: PrepSessionDao, private val steps: StepRecordDao) {
    fun sessionsBetween(from: Long, to: Long): Flow<List<PrepSession>> = sessions.observeBetween(from, to)
    suspend fun startSession(session: PrepSession): Long = sessions.insert(session)
    suspend fun updateSession(session: PrepSession) = sessions.update(session)
    suspend fun session(id: Long): PrepSession? = sessions.get(id)
    suspend fun sessionForEvent(eventId: Long): PrepSession? = sessions.forEvent(eventId)
    /** Ids de eventos para los que la persona ya salió. */
    fun leftEventIds(): Flow<Set<Long>> = sessions.observeLeftEventIds().map { it.toSet() }
    suspend fun recordsFor(sessionId: Long): List<StepRecord> = steps.forSession(sessionId)
    suspend fun record(step: StepRecord): Long = steps.insert(step)
    suspend fun history(routineId: Long, step: PrepStep): List<StepRecord> = steps.history(routineId, step)
}
