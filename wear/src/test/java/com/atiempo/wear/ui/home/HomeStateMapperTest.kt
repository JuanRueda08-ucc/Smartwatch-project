package com.atiempo.wear.ui.home

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class HomeStateMapperTest {
    private val zone = ZoneId.of("America/Bogota")
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 11, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    // Clase: 10 + 15 + 5 + 12 de trayecto → margen 5 → empieza 47 min antes del evento.
    private val clase = Routine(
        id = 1, name = "Clase", items = emptyList(),
        defaultFinishMin = 10, defaultPrepareMin = 15, defaultCheckMin = 7, defaultTravelMin = 12, manualCheckMin = 5,
    )
    private fun event(id: Long, startAt: Long, title: String = "Evento $id") =
        Event(id, title, startAt, routineId = 1, place = null, baseTravelMin = 12, createdByVoice = false)

    private val now = at(16, 7, 0) // lunes 16, 7:00

    @Test
    fun `sin eventos muestra el estado vacio`() {
        assertEquals(HomeUiState.Empty, HomeStateMapper.map(emptyList(), listOf(clase), now, zone))
    }

    @Test
    fun `ignora eventos pasados y elige el mas cercano`() {
        val events = listOf(event(1, at(16, 6)), event(3, at(16, 14)), event(2, at(16, 8), "Clase de cálculo"))
        val state = HomeStateMapper.map(events, listOf(clase), now, zone) as HomeUiState.Upcoming
        assertEquals(2L, state.eventId)
        assertEquals("Clase de cálculo", state.title)
    }

    @Test
    fun `cuenta los minutos hasta empezar a prepararse`() {
        // Evento 8:00 → empezar a prepararse 7:13 → faltan 13 min a las 7:00
        val state = HomeStateMapper.map(listOf(event(1, at(16, 8))), listOf(clase), now, zone) as HomeUiState.Upcoming
        assertEquals(13, state.minutesUntilPrep)
        assertEquals(1 - 13f / 60, state.ringProgress, 0.001f)
        assertFalse(state.isDue)
    }

    @Test
    fun `los segundos sobrantes redondean hacia arriba`() {
        val state = HomeStateMapper.map(listOf(event(1, at(16, 8))), listOf(clase), now + 30_000, zone) as HomeUiState.Upcoming
        assertEquals(13, state.minutesUntilPrep) // faltan 12,5 → se muestra 13
    }

    @Test
    fun `cuando ya es hora queda en cero y el anillo lleno`() {
        val state = HomeStateMapper.map(listOf(event(1, at(16, 8))), listOf(clase), at(16, 7, 20), zone) as HomeUiState.Upcoming
        assertEquals(0, state.minutesUntilPrep)
        assertEquals(1f, state.ringProgress)
        assertTrue(state.isDue)
    }

    @Test
    fun `lejos del evento el anillo esta vacio`() {
        val state = HomeStateMapper.map(listOf(event(1, at(16, 14))), listOf(clase), now, zone) as HomeUiState.Upcoming
        assertEquals(0f, state.ringProgress)
        assertEquals(0, state.dayOffset)
    }

    @Test
    fun `dia del evento hoy manana u otro`() {
        val tomorrow = HomeStateMapper.map(listOf(event(1, at(17, 9))), listOf(clase), now, zone) as HomeUiState.Upcoming
        assertEquals(1, tomorrow.dayOffset)
        val thursday = HomeStateMapper.map(listOf(event(1, at(19, 15, 30))), listOf(clase), now, zone) as HomeUiState.Upcoming
        assertEquals(3, thursday.dayOffset)
        assertEquals(DayOfWeek.THURSDAY, thursday.weekday)
    }

    @Test
    fun `ignora eventos con rutina desconocida`() {
        val orphan = event(1, at(16, 8)).copy(routineId = 99)
        assertEquals(HomeUiState.Empty, HomeStateMapper.map(listOf(orphan), listOf(clase), now, zone))
    }
}
