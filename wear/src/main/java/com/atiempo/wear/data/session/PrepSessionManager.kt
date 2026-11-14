package com.atiempo.wear.data.session

import com.atiempo.wear.alerts.AlarmScheduler
import com.atiempo.wear.alerts.AlertNotifier
import com.atiempo.wear.alerts.Haptics
import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.PrepSession
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.data.db.StepRecord
import com.atiempo.wear.data.repo.EventRepository
import com.atiempo.wear.data.repo.HistoryRepository
import com.atiempo.wear.data.repo.RoutineRepository
import com.atiempo.wear.data.repo.planFor
import com.atiempo.wear.domain.HapticPattern
import com.atiempo.wear.domain.PrepEffect
import com.atiempo.wear.domain.PrepEvent
import com.atiempo.wear.domain.PrepPhase
import com.atiempo.wear.domain.PrepPlan
import com.atiempo.wear.domain.PrepState
import com.atiempo.wear.domain.PrepStateMachine
import com.atiempo.wear.domain.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

/** La preparación en curso: el evento, su plan y el estado del motor. */
data class ActiveSession(
    val sessionId: Long,
    val event: Event,
    val routine: Routine,
    val plan: PrepPlan,
    val machine: PrepStateMachine,
    val state: PrepState,
    /** Objetos ya revisados en "Revisa tus cosas" (índices de `routine.items`). */
    val checkedItems: Set<Int> = emptySet(),
)

/**
 * Mantiene viva la preparación en curso (una a la vez) y ejecuta los efectos del motor de estados:
 * vibrar, publicar el aviso, guardar la duración de cada "Listo" y cerrar la sesión al salir (D-023, D-024).
 *
 * - Con la app abierta, cada segundo avanza el motor y, si no hay sesión, arranca sola la del evento cuyo
 *   primer aviso llegó.
 * - Con la app cerrada, una alarma exacta a la próxima hora en que algo cambia despierta la app
 *   (`AlertReceiver` → `onAlarm`).
 * - Si el sistema cerró la app a mitad de una preparación, al volver a abrirse la recupera de la base.
 */
class PrepSessionManager(
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val zone: ZoneId,
    private val events: EventRepository,
    private val routines: RoutineRepository,
    private val history: HistoryRepository,
    private val haptics: Haptics,
    private val alarms: AlarmScheduler,
    private val notifier: AlertNotifier,
) {
    private val mutex = Mutex()
    private val ready = CompletableDeferred<Unit>()
    private val _active = MutableStateFlow<ActiveSession?>(null)
    val active: StateFlow<ActiveSession?> = _active.asStateFlow()

    private val _autoStarted = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    /** Id de sesión que arrancó sola: la navegación abre su pantalla (como el aviso a pantalla completa). */
    val autoStarted: SharedFlow<Long> = _autoStarted

    private var upcoming: List<Event> = emptyList()
    private var routineList: List<Routine> = emptyList()
    /** Eventos que ya tienen sesión, para no arrancarlos dos veces. */
    private val handledEvents = mutableSetOf<Long>()
    private var scheduledAt: Long? = null

    fun launch() {
        scope.launch {
            // Primero los datos y la sesión guardada; después el ciclo de cada segundo.
            val first = combine(events.upcoming(from = clock.now() - DAY_MS), routines.all()) { e, r -> e to r }.first()
            upcoming = first.first
            routineList = first.second
            mutex.withLock { restore() }
            ready.complete(Unit)
            scope.launch {
                combine(events.upcoming(from = clock.now() - DAY_MS), routines.all()) { e, r -> e to r }
                    .collect { (e, r) -> upcoming = e; routineList = r }
            }
            while (true) {
                tick()
                delay(TICK_MS)
            }
        }
    }

    /** Sonó la alarma: avanza la preparación y programa la siguiente. */
    suspend fun onAlarm() {
        ready.await()
        tick()
    }

    /** "Empezar ahora" desde el Detalle. Si ya hay una preparación en curso, devuelve esa. */
    suspend fun start(eventId: Long): Long? = mutex.withLock {
        _active.value?.takeIf { it.state.phase == PrepPhase.ACTIVE }?.let { return it.sessionId }
        val event = events.get(eventId) ?: return null
        begin(event, manual = true).also { reschedule(clock.now()) }
    }

    /** "Listo" en los estados 1 a 3, "Ya salí" en el 4. */
    fun confirm() {
        scope.launch {
            mutex.withLock {
                _active.value?.let { apply(it, PrepEvent.Confirm(clock.now())) }
                reschedule(clock.now())
            }
        }
    }

    fun toggleItem(index: Int) = _active.update { session ->
        session?.copy(checkedItems = session.checkedItems.let { if (index in it) it - index else it + index })
    }

    /** La pantalla de resultado ya leyó la sesión terminada: se libera para el próximo evento. */
    fun dismissFinished(sessionId: Long) = _active.update { session ->
        if (session?.sessionId == sessionId && session.state.phase == PrepPhase.DONE) null else session
    }

    // ---------- Interno ----------

    private suspend fun tick() = mutex.withLock {
        val now = clock.now()
        val current = _active.value
        if (current == null) {
            val event = AutoStart.eventToStart(upcoming, routineList, now, handledEvents)
            if (event != null) {
                // Si ya tiene sesión guardada (por ejemplo, terminada antes de reiniciar la app), no se repite.
                if (history.sessionForEvent(event.id) != null) {
                    handledEvents += event.id
                } else {
                    begin(event, manual = false)?.let { _autoStarted.tryEmit(it) }
                }
            }
        } else if (current.state.phase == PrepPhase.ACTIVE) {
            apply(current, PrepEvent.Tick(now))
        }
        reschedule(now)
    }

    /** Recupera la preparación guardada sin "Ya salí" si su evento todavía no empieza. */
    private suspend fun restore() {
        val now = clock.now()
        upcoming.forEach { event -> if (history.sessionForEvent(event.id) != null) handledEvents += event.id }
        val open = history.openSession() ?: return
        // Si ese evento tiene una sesión más reciente (por ejemplo, se empezó de nuevo y se terminó),
        // la abierta quedó abandonada: no se recupera.
        if (history.sessionForEvent(open.eventId)?.id != open.id) return
        val event = events.get(open.eventId)?.takeIf { now < it.startAt } ?: return
        val routine = routineList.firstOrNull { it.id == event.routineId } ?: routines.get(event.routineId) ?: return
        val plan = routine.planFor(event)
        val machine = PrepStateMachine(plan)
        handledEvents += event.id
        _active.value = ActiveSession(open.id, event, routine, plan, machine, machine.resume(open.startedAt, now))
    }

    /** Deja programada la alarma a la próxima hora en que algo cambia; solo si cambió. */
    private fun reschedule(now: Long) {
        val session = _active.value
        val next = when {
            session == null -> AutoStart.nextFirstAlertAt(upcoming, routineList, now, handledEvents)
            session.state.phase == PrepPhase.ACTIVE -> session.machine.nextWakeUp(session.state, now)
            else -> null
        }
        if (next != scheduledAt) {
            alarms.schedule(next)
            scheduledAt = next
        }
    }

    private suspend fun begin(event: Event, manual: Boolean): Long? {
        val routine = routineList.firstOrNull { it.id == event.routineId } ?: routines.get(event.routineId) ?: return null
        val plan = routine.planFor(event)
        val machine = PrepStateMachine(plan)
        val now = clock.now()
        val transition = machine.reduce(PrepState(), if (manual) PrepEvent.Start(now) else PrepEvent.Tick(now))
        if (transition.state.phase != PrepPhase.ACTIVE) return null
        val sessionId = history.startSession(PrepSession(eventId = event.id, startedAt = transition.state.sessionStartedAt ?: now))
        handledEvents += event.id
        val session = ActiveSession(sessionId, event, routine, plan, machine, transition.state)
        _active.value = session
        runEffects(session, transition.effects)
        return sessionId
    }

    private suspend fun apply(session: ActiveSession, event: PrepEvent) {
        val transition = session.machine.reduce(session.state, event)
        if (transition.state == session.state && transition.effects.isEmpty()) return
        val updated = session.copy(state = transition.state)
        _active.value = updated
        runEffects(updated, transition.effects)
    }

    private suspend fun runEffects(session: ActiveSession, effects: List<PrepEffect>) {
        effects.forEach { effect ->
            when (effect) {
                is PrepEffect.StepStarted ->
                    if (effect.auto) {
                        haptics.play(HapticPattern.of(effect.step))
                        notify(session)
                    } else {
                        // La persona está mirando el reloj: sin aviso, solo el clic y el patrón del estado nuevo.
                        notifier.cancel()
                        haptics.play(HapticPattern.CONFIRM)
                        scope.launch {
                            delay(CONFIRM_THEN_STEP_MS)
                            haptics.play(HapticPattern.of(effect.step))
                        }
                    }
                is PrepEffect.StepCompleted -> history.record(
                    StepRecord(
                        sessionId = session.sessionId,
                        routineId = session.routine.id,
                        step = effect.duration.step,
                        dayOfWeek = Instant.ofEpochMilli(session.event.startAt).atZone(zone).dayOfWeek.value,
                        plannedMin = effect.duration.plannedMin,
                        actualMin = effect.duration.actualMin,
                    ),
                )
                is PrepEffect.LateReminder -> {
                    haptics.play(HapticPattern.LATE)
                    notify(session)
                }
                is PrepEffect.Finished -> {
                    notifier.cancel()
                    haptics.play(HapticPattern.CONFIRM)
                    history.session(session.sessionId)?.let {
                        history.updateSession(it.copy(leftAt = effect.leftAt, minutesLate = effect.minutesLate))
                    }
                }
            }
        }
    }

    private fun notify(session: ActiveSession) {
        val view = session.machine.view(session.state, clock.now()) ?: return
        notifier.show(view.step, session.event.title, view.minutesLeft, view.isLate, view.minutesLate)
    }

    private companion object {
        const val TICK_MS = 1_000L
        const val CONFIRM_THEN_STEP_MS = 350L
        const val DAY_MS = 24 * 60 * 60 * 1_000L
    }
}
