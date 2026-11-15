package com.atiempo.wear.data.repo

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.data.db.StepRecord
import com.atiempo.wear.domain.model.PrepStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class LearningTest {
    private val zone = ZoneId.of("America/Bogota")
    private val event = Event(1, "Clase", 1_794_834_000_000L, routineId = 1, place = null, baseTravelMin = 12, createdByVoice = false)
    private fun routine(prepare: Int? = null) = Routine(
        id = 1, name = "Clase", items = emptyList(),
        defaultFinishMin = 10, defaultPrepareMin = 15, defaultCheckMin = 7, defaultTravelMin = 12,
        manualPrepareMin = prepare, manualCheckMin = null, manualTravelMin = null,
    )
    private fun prepare(sessionId: Long, minutes: Int) =
        StepRecord(sessionId = sessionId, routineId = 1, step = PrepStep.PREPARE, dayOfWeek = 3, plannedMin = 15, actualMin = minutes)

    private val past = List(5) { prepare(sessionId = it.toLong() + 1, minutes = 15) }

    @Test
    fun `tardar mas adelanta el proximo aviso`() {
        // EMA: 0.3 · 25 + 0.7 · 15 = 18 → 3 min más
        val shift = routine().nextTimeShift(event, past + prepare(99, 25), sessionId = 99, zone = zone)
        assertTrue("debía avisar antes, fue $shift", shift >= 3)
    }

    @Test
    fun `tardar menos atrasa el proximo aviso`() {
        val shift = routine().nextTimeShift(event, past + prepare(99, 5), sessionId = 99, zone = zone)
        assertTrue("debía avisar después, fue $shift", shift < 0)
    }

    @Test
    fun `tardar lo mismo deja la rutina igual`() {
        assertEquals(0, routine().nextTimeShift(event, past + prepare(99, 15), sessionId = 99, zone = zone))
    }

    @Test
    fun `con ajuste manual lo aprendido no mueve el aviso`() {
        assertEquals(0, routine(prepare = 20).nextTimeShift(event, past + prepare(99, 40), sessionId = 99, zone = zone))
    }

    @Test
    fun `la primera vez tambien aprende`() {
        val shift = routine().nextTimeShift(event, listOf(prepare(99, 25)), sessionId = 99, zone = zone)
        assertTrue("debía avisar antes, fue $shift", shift >= 10)
    }
}
