package com.atiempo.wear.domain

import com.atiempo.wear.domain.model.PrepStep
import org.junit.Assert.assertEquals
import org.junit.Test

class PrepPlannerTest {
    private val minute = 60_000L
    private val eventAt = 8 * 60 * minute // 8:00 de un día cualquiera

    private fun clock(millis: Long) = "%d:%02d".format(millis / minute / 60, millis / minute % 60)

    @Test
    fun `margen del 10 por ciento redondeado hacia arriba`() {
        assertEquals(5, PrepPlanner.margin(StepDurations(10, 15, 5, 12))) // 42 → 4,2 → 5
        assertEquals(5, PrepPlanner.margin(StepDurations(10, 20, 10, 10))) // 50 → 5 exacto
    }

    @Test
    fun `margen minimo de 3 minutos`() {
        assertEquals(3, PrepPlanner.margin(StepDurations(2, 3, 2, 5)))
        assertEquals(3, PrepPlanner.margin(StepDurations(0, 0, 0, 0)))
    }

    @Test
    fun `el plan se calcula hacia atras desde el evento`() {
        val plan = PrepPlanner.plan(eventAt, StepDurations(finish = 10, prepare = 15, check = 5, travel = 12))
        assertEquals("7:43", clock(plan.leaveAt))   // 8:00 − 12 − 5
        assertEquals("7:38", clock(plan.checkAt))
        assertEquals("7:23", clock(plan.prepareAt))
        assertEquals("7:13", clock(plan.finishAt))
        assertEquals("7:55", clock(plan.arriveAt))  // llega con el margen antes del evento
        assertEquals(5, plan.marginMin)
    }

    @Test
    fun `los inicios van en orden`() {
        val plan = PrepPlanner.plan(eventAt, StepDurations(10, 20, 5, 18))
        val starts = PrepStep.entries.map { plan.startOf(it) }
        assertEquals(starts.sorted(), starts)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza duraciones negativas`() {
        PrepPlanner.plan(eventAt, StepDurations(-1, 10, 5, 10))
    }
}
