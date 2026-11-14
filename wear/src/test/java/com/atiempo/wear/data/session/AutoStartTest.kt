package com.atiempo.wear.data.session

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoStartTest {
    private val minute = 60_000L
    private fun at(hour: Int, min: Int) = (hour * 60L + min) * minute

    // Clase: 10 + 15 + 5 + 12 → margen 5 → primer aviso 47 min antes del evento.
    private val clase = Routine(
        id = 1, name = "Clase", items = emptyList(),
        defaultFinishMin = 10, defaultPrepareMin = 15, defaultCheckMin = 7, defaultTravelMin = 12, manualCheckMin = 5,
    )
    private fun event(id: Long, startAt: Long) =
        Event(id, "Evento $id", startAt, routineId = 1, place = null, baseTravelMin = 12, createdByVoice = false)

    @Test
    fun `antes del primer aviso no arranca`() {
        assertNull(AutoStart.eventToStart(listOf(event(1, at(8, 0))), listOf(clase), at(7, 12), emptySet()))
    }

    @Test
    fun `al llegar el primer aviso arranca`() {
        assertEquals(1L, AutoStart.eventToStart(listOf(event(1, at(8, 0))), listOf(clase), at(7, 13), emptySet())?.id)
    }

    @Test
    fun `no vuelve a arrancar un evento con sesion`() {
        assertNull(AutoStart.eventToStart(listOf(event(1, at(8, 0))), listOf(clase), at(7, 20), setOf(1L)))
    }

    @Test
    fun `un evento que ya empezo no arranca`() {
        assertNull(AutoStart.eventToStart(listOf(event(1, at(8, 0))), listOf(clase), at(8, 0), emptySet()))
    }

    @Test
    fun `entre varios elige el mas proximo`() {
        val events = listOf(event(2, at(8, 30)), event(1, at(8, 0)))
        assertEquals(1L, AutoStart.eventToStart(events, listOf(clase), at(7, 50), emptySet())?.id)
    }
}
