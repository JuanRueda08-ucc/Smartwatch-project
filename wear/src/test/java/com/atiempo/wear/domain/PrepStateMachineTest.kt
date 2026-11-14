package com.atiempo.wear.domain

import com.atiempo.wear.domain.PrepEffect.Finished
import com.atiempo.wear.domain.PrepEffect.LateReminder
import com.atiempo.wear.domain.PrepEffect.StepCompleted
import com.atiempo.wear.domain.PrepEffect.StepStarted
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.domain.model.PrepStep.CHECK
import com.atiempo.wear.domain.model.PrepStep.FINISH
import com.atiempo.wear.domain.model.PrepStep.LEAVE
import com.atiempo.wear.domain.model.PrepStep.PREPARE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrepStateMachineTest {
    private val minute = 60_000L
    private fun at(hour: Int, min: Int, sec: Int = 0) = (hour * 60L + min) * minute + sec * 1_000L

    // Evento 8:00; 10 + 15 + 5 + 12 de trayecto → margen 5.
    // Avisos: 7:13 Termina, 7:23 Prepárate, 7:38 Revisa, 7:43 Sal. Hora límite: 7:48.
    private val plan = PrepPlanner.plan(at(8, 0), StepDurations(finish = 10, prepare = 15, check = 5, travel = 12))
    private val machine = PrepStateMachine(plan)

    /** Aplica eventos en orden y devuelve el estado final y todos los efectos. */
    private fun run(vararg events: PrepEvent, from: PrepState = PrepState()): Pair<PrepState, List<PrepEffect>> {
        var state = from
        val effects = mutableListOf<PrepEffect>()
        events.forEach { e -> machine.reduce(state, e).also { state = it.state; effects += it.effects } }
        return state to effects
    }

    @Test
    fun `antes del primer aviso sigue en reposo`() {
        val (state, effects) = run(PrepEvent.Tick(at(7, 0)), PrepEvent.Confirm(at(7, 5)))
        assertEquals(PrepPhase.IDLE, state.phase)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `el primer aviso empieza Termina a su hora`() {
        val (state, effects) = run(PrepEvent.Tick(at(7, 13, 2)))
        assertEquals(PrepPhase.ACTIVE, state.phase)
        assertEquals(FINISH, state.step)
        assertEquals(at(7, 13), state.stepStartedAt) // la hora del plan, no la del tic
        assertEquals(listOf(StepStarted(FINISH, auto = true)), effects)
    }

    @Test
    fun `sin Listo cada paso pasa solo al siguiente`() {
        val (state, effects) = run(
            PrepEvent.Tick(at(7, 13)), PrepEvent.Tick(at(7, 23)), PrepEvent.Tick(at(7, 38)), PrepEvent.Tick(at(7, 43)),
        )
        assertEquals(LEAVE, state.step)
        assertEquals(listOf(FINISH, PREPARE, CHECK, LEAVE), effects.filterIsInstance<StepStarted>().map { it.step })
        assertTrue(effects.filterIsInstance<StepStarted>().all { it.auto })
        assertTrue("sin Listo no se registra duración", effects.none { it is StepCompleted })
    }

    @Test
    fun `si la app estuvo dormida salta al paso que toca y avisa una sola vez`() {
        val (state, effects) = run(PrepEvent.Tick(at(7, 13)), PrepEvent.Tick(at(7, 40)))
        assertEquals(CHECK, state.step)
        assertEquals(at(7, 38), state.stepStartedAt)
        assertEquals(listOf(StepStarted(FINISH, true), StepStarted(CHECK, true)), effects)
    }

    @Test
    fun `al abrir tarde la app empieza en el paso que toca`() {
        val (state, _) = run(PrepEvent.Tick(at(7, 30)))
        assertEquals(PREPARE, state.step)
        assertEquals(at(7, 23), state.sessionStartedAt)
    }

    @Test
    fun `Listo antes de tiempo registra la duracion real y el siguiente conserva su fin`() {
        val (state, effects) = run(PrepEvent.Tick(at(7, 13)), PrepEvent.Confirm(at(7, 20)))
        assertEquals(PREPARE, state.step)
        assertEquals(at(7, 20), state.stepStartedAt)
        assertEquals(StepCompleted(StepDuration(FINISH, plannedMin = 10, actualMin = 7)), effects[1])
        assertEquals(StepStarted(PREPARE, auto = false), effects[2])
        // Prepárate sigue terminando a las 7:38: ahora tiene 18 min en vez de 15.
        assertEquals(18, machine.view(state, at(7, 20))!!.minutesLeft)
    }

    @Test
    fun `Empezar ahora arranca antes de la hora`() {
        val (state, effects) = run(PrepEvent.Start(at(7, 0)))
        assertEquals(FINISH, state.step)
        assertEquals(at(7, 0), state.stepStartedAt)
        assertEquals(listOf(StepStarted(FINISH, auto = false)), effects)
        assertEquals(23, machine.view(state, at(7, 0))!!.minutesLeft) // termina a las 7:23
    }

    @Test
    fun `Ya sali a tiempo termina con margen`() {
        val (state, effects) = run(PrepEvent.Tick(at(7, 43)), PrepEvent.Confirm(at(7, 45)))
        assertEquals(PrepPhase.DONE, state.phase)
        assertEquals(-3, state.minutesLate) // salió 3 min antes de la hora límite
        assertEquals(Finished(at(7, 45), -3), effects.last())
    }

    @Test
    fun `pasada la hora limite va tarde con 3 recordatorios cada minuto`() {
        val (state, effects) = run(
            PrepEvent.Tick(at(7, 43)),
            PrepEvent.Tick(at(7, 48)), // 1
            PrepEvent.Tick(at(7, 48, 30)), // aún no pasa el minuto
            PrepEvent.Tick(at(7, 49)), // 2
            PrepEvent.Tick(at(7, 50)), // 3
            PrepEvent.Tick(at(7, 55)), // ya no más
        )
        assertEquals(listOf(1, 2, 3), effects.filterIsInstance<LateReminder>().map { it.count })
        val view = machine.view(state, at(7, 55))!!
        assertTrue(view.isLate)
        assertEquals(7, view.minutesLate)
        assertEquals(0f, view.remainingFraction)
    }

    @Test
    fun `Ya sali tarde cuenta los minutos de retraso`() {
        val (state, _) = run(PrepEvent.Tick(at(7, 43)), PrepEvent.Confirm(at(7, 51, 10)))
        assertEquals(4, state.minutesLate) // 3 min 10 s tarde → 4
    }

    @Test
    fun `Listo en el estado 4 es Ya sali y despues nada cambia`() {
        val (done, _) = run(PrepEvent.Tick(at(7, 43)), PrepEvent.Confirm(at(7, 44)))
        val (after, effects) = run(PrepEvent.Tick(at(8, 0)), PrepEvent.Confirm(at(8, 1)), from = done)
        assertEquals(done, after)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `Saltar no registra duracion`() {
        val (state, effects) = run(PrepEvent.Tick(at(7, 13)), PrepEvent.Skip(at(7, 15)))
        assertEquals(PREPARE, state.step)
        assertTrue(effects.none { it is StepCompleted })
    }

    @Test
    fun `movimiento hacia la salida lleva al estado 4`() {
        val (state, effects) = run(PrepEvent.Tick(at(7, 25)), PrepEvent.MovementDetected(at(7, 30)))
        assertEquals(LEAVE, state.step)
        assertEquals(StepStarted(LEAVE, auto = true), effects.last())
    }

    @Test
    fun `la vista cuenta minutos y anillo del paso`() {
        val (state, _) = run(PrepEvent.Tick(at(7, 23)))
        val view = machine.view(state, at(7, 30, 30))!!
        assertEquals(PREPARE, view.step)
        assertEquals(8, view.minutesLeft) // faltan 7,5 → 8
        assertEquals(7.5f / 15, view.remainingFraction, 0.001f)
        assertFalse(view.isLate)
        assertNull(machine.view(PrepState(), at(7, 0)))
    }

    @Test
    fun `la proxima alarma sigue el plan`() {
        assertEquals(at(7, 13), machine.nextWakeUp(PrepState(), at(7, 0)))
        val (finish, _) = run(PrepEvent.Tick(at(7, 13)))
        assertEquals(at(7, 23), machine.nextWakeUp(finish, at(7, 15)))
        val (leave, _) = run(PrepEvent.Tick(at(7, 43)))
        assertEquals(at(7, 48), machine.nextWakeUp(leave, at(7, 44))) // hora límite
    }

    @Test
    fun `la proxima alarma de tarde es cada minuto hasta tres`() {
        val (late1, _) = run(PrepEvent.Tick(at(7, 43)), PrepEvent.Tick(at(7, 48)))
        assertEquals(at(7, 49), machine.nextWakeUp(late1, at(7, 48, 10)))
        val (late3, _) = run(PrepEvent.Tick(at(7, 48)), PrepEvent.Tick(at(7, 49)), PrepEvent.Tick(at(7, 50)), from = late1)
        assertNull(machine.nextWakeUp(late3, at(7, 50, 5)))
    }

    @Test
    fun `sin alarmas despues de salir`() {
        val (done, _) = run(PrepEvent.Tick(at(7, 43)), PrepEvent.Confirm(at(7, 44)))
        assertNull(machine.nextWakeUp(done, at(7, 45)))
    }

    @Test
    fun `al recuperar sigue en el paso que toca`() {
        val resumed = machine.resume(sessionStartedAt = at(7, 13), now = at(7, 40))
        assertEquals(PrepPhase.ACTIVE, resumed.phase)
        assertEquals(CHECK, resumed.step)
        assertEquals(at(7, 38), resumed.stepStartedAt)
        // Si se empezó antes de tiempo ("Empezar ahora") y aún no llega el primer aviso, sigue en Termina.
        val early = machine.resume(sessionStartedAt = at(7, 0), now = at(7, 5))
        assertEquals(FINISH, early.step)
        assertEquals(at(7, 0), early.stepStartedAt)
    }

    @Test
    fun `duraciones reales acotadas entre 1 y 90 minutos`() {
        assertEquals(1, PrepStateMachine.actualMinutes(at(7, 0), at(7, 0, 10)))
        assertEquals(90, PrepStateMachine.actualMinutes(at(7, 0), at(10, 0)))
        assertEquals(PrepStep.entries.size, 4)
    }
}
