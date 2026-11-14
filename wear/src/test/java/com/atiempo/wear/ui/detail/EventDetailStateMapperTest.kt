package com.atiempo.wear.ui.detail

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.domain.model.PrepStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class EventDetailStateMapperTest {
    private val zone = ZoneId.of("America/Bogota")
    private fun at(hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 11, 16, hour, minute).atZone(zone).toInstant().toEpochMilli()
    private fun clock(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime().toString()

    // Clase: 10 + 15 + 5 + 12 → margen 5. Evento 8:00 → 7:13, 7:23, 7:38, 7:43; llega 7:55.
    private val clase = Routine(
        id = 1, name = "Clase", items = emptyList(),
        defaultFinishMin = 10, defaultPrepareMin = 15, defaultCheckMin = 7, defaultTravelMin = 12, manualCheckMin = 5,
    )
    private val event = Event(1, "Clase de cálculo", at(8), routineId = 1, place = "Bloque 7", baseTravelMin = 12, createdByVoice = false)

    private fun loaded(now: Long) = EventDetailStateMapper.map(event, listOf(clase), now, zone) as EventDetailUiState.Loaded

    @Test
    fun `sin evento o sin rutina no se encuentra`() {
        assertEquals(EventDetailUiState.NotFound, EventDetailStateMapper.map(null, listOf(clase), at(7), zone))
        assertEquals(EventDetailUiState.NotFound, EventDetailStateMapper.map(event.copy(routineId = 9), listOf(clase), at(7), zone))
    }

    @Test
    fun `linea de tiempo con las horas de los 4 estados`() {
        val state = loaded(at(6))
        assertEquals(PrepStep.entries, state.steps.map { it.step })
        assertEquals(listOf("07:13", "07:23", "07:38", "07:43"), state.steps.map { clock(it.startAt) })
        assertEquals("07:55", clock(state.arriveAt))
        assertTrue(state.steps.all { it.status == StepStatus.UPCOMING })
        assertTrue(state.canStart)
    }

    @Test
    fun `resalta el paso que toca segun la hora`() {
        val state = loaded(at(7, 40)) // 7:38 a 7:43 es "Revisa tus cosas"
        assertEquals(
            listOf(StepStatus.PAST, StepStatus.PAST, StepStatus.CURRENT, StepStatus.UPCOMING),
            state.steps.map { it.status },
        )
    }

    @Test
    fun `el inicio de un paso ya cuenta como actual`() {
        assertEquals(StepStatus.CURRENT, loaded(at(7, 13)).steps.first().status)
    }

    @Test
    fun `cuando el evento empieza ya no se puede iniciar`() {
        val state = loaded(at(8))
        assertFalse(state.canStart)
        assertTrue(state.steps.all { it.status == StepStatus.PAST })
    }
}
