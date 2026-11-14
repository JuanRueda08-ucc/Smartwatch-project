package com.atiempo.wear.ui.events

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class EventsStateMapperTest {
    private val zone = ZoneId.of("America/Bogota")
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 11, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
    private fun clock(millis: Long) = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone).toLocalTime().toString()

    private val clase = Routine(
        id = 1, name = "Clase", items = emptyList(),
        defaultFinishMin = 10, defaultPrepareMin = 15, defaultCheckMin = 7, defaultTravelMin = 12, manualCheckMin = 5,
    )
    private fun event(id: Long, startAt: Long, place: String? = null) =
        Event(id, "Evento $id", startAt, routineId = 1, place = place, baseTravelMin = 12, createdByVoice = false)

    private val now = at(16, 7, 0) // lunes 16, 7:00

    @Test
    fun `sin eventos proximos la lista queda vacia`() {
        val state = EventsStateMapper.map(listOf(event(1, at(16, 6))), listOf(clase), now, zone)
        assertTrue(state.groups.isEmpty())
    }

    @Test
    fun `agrupa por dia en orden`() {
        val events = listOf(event(3, at(19, 15, 30)), event(2, at(16, 14)), event(1, at(16, 8)), event(4, at(17, 9)))
        val groups = EventsStateMapper.map(events, listOf(clase), now, zone).groups
        assertEquals(listOf(0, 1, 3), groups.map { it.day.dayOffset })
        assertEquals(listOf(1L, 2L), groups[0].events.map { it.id })
        assertEquals(DayOfWeek.THURSDAY, groups[2].day.weekday)
        assertEquals(19, groups[2].day.dayOfMonth)
    }

    @Test
    fun `cada evento trae su hora de salida del plan`() {
        // 8:00 − 12 de trayecto − 5 de margen = 7:43
        val item = EventsStateMapper.map(listOf(event(1, at(16, 8), "Bloque 7")), listOf(clase), now, zone).groups.single().events.single()
        assertEquals("07:43", clock(item.leaveAt))
        assertEquals("Bloque 7", item.place)
    }

    @Test
    fun `el evento de hoy que ya empezo no aparece`() {
        val groups = EventsStateMapper.map(listOf(event(1, at(16, 7)), event(2, at(16, 9))), listOf(clase), now + 1, zone).groups
        assertEquals(listOf(2L), groups.single().events.map { it.id })
    }
}
