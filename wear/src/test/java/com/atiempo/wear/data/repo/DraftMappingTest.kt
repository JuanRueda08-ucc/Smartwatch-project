package com.atiempo.wear.data.repo

import com.atiempo.wear.data.seed.SampleData
import com.atiempo.wear.domain.EventParser
import com.atiempo.wear.domain.model.Parsed
import com.atiempo.wear.domain.model.RoutineKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DraftMappingTest {
    private val zone = ZoneId.of("America/Bogota")
    private val now = LocalDateTime.of(2026, 11, 16, 9, 0)
    private val nowMs = now.atZone(zone).toInstant().toEpochMilli()
    private val routines = SampleData.routines()

    @Test
    fun `cada tipo de rutina apunta a una rutina sembrada`() {
        val ids = routines.map { it.id }.toSet()
        RoutineKind.entries.forEach { assertTrue(it.routineId() in ids) }
    }

    @Test
    fun `el borrador se convierte en evento con hora y trayecto de la rutina`() {
        val draft = EventParser.parse("Cine con Laura el sábado a las 6 de la tarde en el Andino", now)
        val event = draft.toEvent(routines, zone)!!
        assertEquals("Cine con Laura", event.title)
        assertEquals(LocalDateTime.of(2026, 11, 21, 18, 0).atZone(zone).toInstant().toEpochMilli(), event.startAt)
        assertEquals(SampleData.SOCIAL_ID, event.routineId)
        assertEquals("Andino", event.place)
        assertEquals(25, event.baseTravelMin) // trayecto por defecto de Salida social
        assertTrue(event.createdByVoice)
        assertEquals(0L, event.id) // lo asigna Room
    }

    @Test
    fun `sin lugar el evento queda sin lugar`() {
        assertNull(EventParser.parse("Clase mañana a las 7", now).toEvent(routines, zone)!!.place)
    }

    @Test
    fun `sin la rutina en la base no hay evento`() {
        assertNull(EventParser.parse("Clase mañana a las 7", now).toEvent(emptyList(), zone))
    }

    @Test
    fun `detecta si la hora ya paso`() {
        val draft = EventParser.parse("Cita hoy a las 15:00", now)
        assertFalse(draft.isPast(nowMs, zone))
        val earlier = draft.copy(time = Parsed(java.time.LocalTime.of(8, 0), true))
        assertTrue(earlier.isPast(nowMs, zone))
    }
}
