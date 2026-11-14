package com.atiempo.wear.domain

import com.atiempo.wear.domain.model.PrepStep
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Fase de la preparación. */
enum class PrepPhase { IDLE, ACTIVE, DONE }

/** Duración real de un paso confirmado con "Listo", para guardarla como StepRecord. */
data class StepDuration(val step: PrepStep, val plannedMin: Int, val actualMin: Int)

/**
 * Estado del motor. Inmutable: cada evento produce un estado nuevo.
 * Tiempos en epoch ms.
 */
data class PrepState(
    val phase: PrepPhase = PrepPhase.IDLE,
    val step: PrepStep? = null,
    val stepStartedAt: Long? = null,
    val sessionStartedAt: Long? = null,
    val leftAt: Long? = null,
    /** Minutos después de la hora límite de salida (salida + margen). 0 o menos = a tiempo. */
    val minutesLate: Int? = null,
    val lateReminders: Int = 0,
    val lastReminderAt: Long? = null,
)

/** Lo que llega al motor. */
sealed interface PrepEvent {
    val now: Long

    /** Paso del tiempo. Llega cada pocos segundos y cuando suena una alarma. */
    data class Tick(override val now: Long) : PrepEvent

    /** "Empezar ahora" en el Detalle: arranca antes de la hora del primer aviso. */
    data class Start(override val now: Long) : PrepEvent

    /** "Listo" (estados 1 a 3) o "Ya salí" (estado 4). */
    data class Confirm(override val now: Long) : PrepEvent

    /** Saltar el paso actual sin registrar su duración. */
    data class Skip(override val now: Long) : PrepEvent

    /**
     * El reloj detecta que la persona se mueve hacia la salida. Fuera de esta entrega (D-004):
     * existe para mostrar que el diseño lo contempla.
     */
    data class MovementDetected(override val now: Long) : PrepEvent
}

/** Lo que el motor pide hacer afuera: vibrar, guardar, avanzar la pantalla. */
sealed interface PrepEffect {
    /** Empezó un paso. `auto` = por la hora (aviso con su patrón háptico); si no, por la persona. */
    data class StepStarted(val step: PrepStep, val auto: Boolean) : PrepEffect

    /** Un paso se confirmó con "Listo": guardar su duración real. */
    data class StepCompleted(val duration: StepDuration) : PrepEffect

    /** Recordatorio "Vas tarde" número `count` (1 a 3). */
    data class LateReminder(val count: Int) : PrepEffect

    /** La persona confirmó "Ya salí". */
    data class Finished(val leftAt: Long, val minutesLate: Int) : PrepEffect
}

data class PrepTransition(val state: PrepState, val effects: List<PrepEffect> = emptyList())

/** Vista para la pantalla de un estado: cuánto falta, cuánto anillo queda y si va tarde. */
data class PrepView(
    val step: PrepStep,
    /** Minutos que faltan para que termine el paso, redondeados hacia arriba; 0 si ya terminó. */
    val minutesLeft: Int,
    /** Fracción del paso que queda: 1 al empezar, 0 al terminar (para el anillo). */
    val remainingFraction: Float,
    val isLate: Boolean,
    /** Minutos tarde (solo en el estado 4 pasada la hora límite). */
    val minutesLate: Int,
)

/**
 * Motor de los 4 estados (ARQUITECTURA.md). Kotlin puro, sin Android, para probarlo con pruebas unitarias.
 *
 * IDLE → FINISH → PREPARE → CHECK → LEAVE → DONE
 *
 * - Cada paso termina cuando empieza el siguiente según el plan. El último (LEAVE) termina en la hora
 *   límite de salida: la salida planeada más el margen de seguridad.
 * - En los estados 1 a 3, si el tiempo se acaba, se pasa solo al siguiente (D-009). Si se saltaron varios
 *   (la app estuvo dormida), se va directo al que toca y se avisa una sola vez.
 * - "Listo" antes de tiempo registra la duración real; el paso siguiente empieza ya y conserva su hora de fin.
 * - Pasada la hora límite en LEAVE se va tarde: recordatorio en ese momento y cada 60 s, máximo 3.
 */
class PrepStateMachine(private val plan: PrepPlan) {

    /** Hora límite para salir: la salida planeada más el margen. Después de esto se va tarde. */
    val lateAt: Long = plan.leaveAt + plan.marginMin * MINUTE

    fun reduce(state: PrepState, event: PrepEvent): PrepTransition = when (state.phase) {
        PrepPhase.IDLE -> onIdle(state, event)
        PrepPhase.ACTIVE -> onActive(state, event)
        PrepPhase.DONE -> PrepTransition(state)
    }

    /** Hora en que termina un paso según el plan. */
    fun endOf(step: PrepStep): Long = when (step) {
        PrepStep.FINISH -> plan.prepareAt
        PrepStep.PREPARE -> plan.checkAt
        PrepStep.CHECK -> plan.leaveAt
        PrepStep.LEAVE -> lateAt
    }

    /** El paso que toca a una hora dada según el plan (FINISH si todavía no empieza). */
    fun stepAt(now: Long): PrepStep = PrepStep.entries.lastOrNull { plan.startOf(it) <= now } ?: PrepStep.FINISH

    fun view(state: PrepState, now: Long): PrepView? {
        val step = state.step ?: return null
        val start = state.stepStartedAt ?: return null
        val end = endOf(step)
        val left = (end - now).coerceAtLeast(0)
        val total = (end - start).coerceAtLeast(1)
        val late = step == PrepStep.LEAVE && now > lateAt
        return PrepView(
            step = step,
            minutesLeft = ceilMinutes(left),
            remainingFraction = if (late) 0f else (left.toFloat() / total).coerceIn(0f, 1f),
            isLate = late,
            minutesLate = if (late) ceilMinutes(now - lateAt) else 0,
        )
    }

    /**
     * Próxima hora en que algo cambia sin que la persona toque nada: el primer aviso, el fin del paso, la
     * hora límite o el siguiente recordatorio de "tarde". La alarma del reloj se programa a esa hora.
     * null si ya no hay nada que avisar.
     */
    fun nextWakeUp(state: PrepState, now: Long): Long? = when (state.phase) {
        PrepPhase.IDLE -> plan.finishAt.takeIf { it > now }
        PrepPhase.DONE -> null
        PrepPhase.ACTIVE -> when {
            state.step != PrepStep.LEAVE -> maxOf(endOf(state.step ?: PrepStep.FINISH), now + 1)
            now < lateAt -> lateAt
            state.lateReminders >= MAX_LATE_REMINDERS -> null
            // Pasada la hora límite sin recordatorio todavía: toca ya.
            state.lateReminders == 0 || state.lastReminderAt == null -> now + 1
            else -> maxOf(state.lastReminderAt + LATE_REMINDER_EVERY, now + 1)
        }
    }

    /**
     * Rehace el estado de una preparación guardada cuando el sistema cerró la app: el paso es el que toca
     * según la hora (las confirmaciones hechas antes se pierden, pero sus duraciones ya están guardadas).
     */
    fun resume(sessionStartedAt: Long, now: Long): PrepState {
        val firstStep = stepAt(sessionStartedAt)
        val step = maxOf(stepAt(now), firstStep)
        // Si sigue en el paso con que empezó, ese paso empezó cuando empezó la sesión (quizá antes de tiempo);
        // si no, a la hora del plan.
        val stepStart = if (step == firstStep) sessionStartedAt else maxOf(plan.startOf(step), sessionStartedAt)
        return PrepState(
            phase = PrepPhase.ACTIVE,
            step = step,
            stepStartedAt = stepStart.coerceAtMost(now),
            sessionStartedAt = sessionStartedAt,
        )
    }

    // ---------- Transiciones ----------

    private fun onIdle(state: PrepState, event: PrepEvent): PrepTransition = when (event) {
        is PrepEvent.Tick -> {
            if (event.now < plan.finishAt) {
                PrepTransition(state)
            } else {
                // Llegó la hora del primer aviso (o de uno posterior, si la app estaba dormida).
                val step = stepAt(event.now)
                val startedAt = plan.startOf(step)
                val started = state.copy(phase = PrepPhase.ACTIVE, step = step, stepStartedAt = startedAt, sessionStartedAt = startedAt)
                withLateCheck(started, event.now, listOf(PrepEffect.StepStarted(step, auto = true)))
            }
        }
        is PrepEvent.Start -> {
            val step = stepAt(event.now)
            PrepTransition(
                state.copy(phase = PrepPhase.ACTIVE, step = step, stepStartedAt = event.now, sessionStartedAt = event.now),
                listOf(PrepEffect.StepStarted(step, auto = false)),
            )
        }
        else -> PrepTransition(state)
    }

    private fun onActive(state: PrepState, event: PrepEvent): PrepTransition {
        val step = state.step ?: return PrepTransition(state)
        return when (event) {
            is PrepEvent.Tick -> onTick(state, step, event.now)
            is PrepEvent.Confirm -> if (step == PrepStep.LEAVE) finish(state, event.now) else confirm(state, step, event.now)
            is PrepEvent.Skip -> if (step == PrepStep.LEAVE) PrepTransition(state) else startNext(state, step, event.now, auto = false)
            is PrepEvent.MovementDetected ->
                if (step == PrepStep.LEAVE) PrepTransition(state)
                else PrepTransition(
                    state.copy(step = PrepStep.LEAVE, stepStartedAt = event.now),
                    listOf(PrepEffect.StepStarted(PrepStep.LEAVE, auto = true)),
                )
            is PrepEvent.Start -> PrepTransition(state)
        }
    }

    private fun onTick(state: PrepState, step: PrepStep, now: Long): PrepTransition {
        if (step != PrepStep.LEAVE && now >= endOf(step)) {
            // Se acabó el tiempo sin "Listo": va al paso que toca ahora, con su hora de inicio del plan.
            val next = maxOf(stepAt(now), PrepStep.entries[step.ordinal + 1])
            val advanced = state.copy(step = next, stepStartedAt = maxOf(plan.startOf(next), state.stepStartedAt ?: 0))
            return withLateCheck(advanced, now, listOf(PrepEffect.StepStarted(next, auto = true)))
        }
        return withLateCheck(state, now, emptyList())
    }

    /** En LEAVE y pasada la hora límite: recordatorio "Vas tarde" al llegar y cada 60 s, máximo 3. */
    private fun withLateCheck(state: PrepState, now: Long, effects: List<PrepEffect>): PrepTransition {
        if (state.step != PrepStep.LEAVE || now < lateAt || state.lateReminders >= MAX_LATE_REMINDERS) {
            return PrepTransition(state, effects)
        }
        val last = state.lastReminderAt
        if (last != null && now - last < LATE_REMINDER_EVERY) return PrepTransition(state, effects)
        val count = state.lateReminders + 1
        return PrepTransition(
            state.copy(lateReminders = count, lastReminderAt = now),
            effects + PrepEffect.LateReminder(count),
        )
    }

    private fun confirm(state: PrepState, step: PrepStep, now: Long): PrepTransition {
        val duration = StepDuration(step, plannedMinutes(step), actualMinutes(state.stepStartedAt ?: now, now))
        val next = startNext(state, step, now, auto = false)
        return next.copy(effects = listOf(PrepEffect.StepCompleted(duration)) + next.effects)
    }

    private fun startNext(state: PrepState, step: PrepStep, now: Long, auto: Boolean): PrepTransition {
        val next = PrepStep.entries[step.ordinal + 1]
        return PrepTransition(state.copy(step = next, stepStartedAt = now), listOf(PrepEffect.StepStarted(next, auto)))
    }

    private fun finish(state: PrepState, now: Long): PrepTransition {
        val minutesLate = if (now > lateAt) ceilMinutes(now - lateAt) else -((lateAt - now) / MINUTE).toInt()
        return PrepTransition(
            state.copy(phase = PrepPhase.DONE, leftAt = now, minutesLate = minutesLate),
            listOf(PrepEffect.Finished(now, minutesLate)),
        )
    }

    private fun plannedMinutes(step: PrepStep): Int = ((endOf(step) - plan.startOf(step)) / MINUTE).toInt()

    companion object {
        const val MINUTE = 60_000L
        const val MAX_LATE_REMINDERS = 3
        const val LATE_REMINDER_EVERY = MINUTE

        /** Duraciones reales acotadas entre 1 y 90 min para descartar olvidos (MODELO_ESTIMACION). */
        fun actualMinutes(from: Long, to: Long): Int = ((to - from) / MINUTE.toDouble()).roundToInt().coerceIn(1, 90)

        private fun ceilMinutes(millis: Long): Int = ceil(millis / MINUTE.toDouble() - 1e-9).toInt().coerceAtLeast(0)
    }
}
