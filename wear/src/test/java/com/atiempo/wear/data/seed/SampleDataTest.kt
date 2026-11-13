package com.atiempo.wear.data.seed

import com.atiempo.wear.domain.model.PrepStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SampleDataTest {

    private val zone = ZoneId.of("America/Bogota")
    private val now = LocalDateTime.of(2026, 11, 17, 9, 20) // martes, día de la entrega
    private val seed = SampleData.generate(now, zone)

    private fun dateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    private val eventsById = seed.events.associateBy { it.id }

    @Test
    fun `es determinista con la misma fecha`() {
        assertEquals(seed, SampleData.generate(now, zone))
    }

    @Test
    fun `funciona empezando cualquier dia de la semana`() {
        (0L..6L).forEach { offset ->
            val other = SampleData.generate(now.plusDays(offset), zone)
            val lastWeek = other.sessions.filter { dateOf(it.startedAt) >= now.toLocalDate().plusDays(offset - 7) }
            assertEquals("salidas de la última semana (día +$offset)", 6, lastWeek.size)
            assertEquals("a tiempo en la última semana (día +$offset)", 5, lastWeek.count { (it.minutesLate ?: 0) <= 0 })
        }
    }

    @Test
    fun `tres rutinas con objetos`() {
        assertEquals(listOf("Clase", "Cita", "Salida social"), seed.routines.map { it.name })
        seed.routines.forEach { assertTrue(it.items.isNotEmpty()) }
    }

    @Test
    fun `tres eventos proximos con ids 1 a 3`() {
        val upcoming = seed.events.filter { it.startAt > now.atZone(zone).toInstant().toEpochMilli() }
        assertEquals(listOf(1L, 2L, 3L), upcoming.map { it.id })
    }

    @Test
    fun `el historial cubre los 14 dias anteriores`() {
        val dates = seed.sessions.map { dateOf(it.startedAt) }
        assertEquals(now.toLocalDate().minusDays(SampleData.HISTORY_DAYS.toLong()), dates.min())
        assertEquals(now.toLocalDate().minusDays(1), dates.max())
    }

    @Test
    fun `cada sesion tiene sus 4 pasos y datos coherentes`() {
        val bySession = seed.records.groupBy { it.sessionId }
        seed.sessions.forEach { session ->
            val event = eventsById.getValue(session.eventId)
            val records = bySession.getValue(session.id)
            assertEquals(PrepStep.entries, records.map { it.step })
            records.forEach {
                assertEquals(event.routineId, it.routineId)
                assertEquals(dateOf(event.startAt).dayOfWeek.value, it.dayOfWeek)
                assertTrue("duración ${it.actualMin}", it.actualMin in 1..90)
            }
        }
    }

    @Test
    fun `los lunes prepararse para clase toma unos 6 min mas`() {
        val prepare = seed.records.filter { it.routineId == SampleData.CLASE_ID && it.step == PrepStep.PREPARE }
        val (mondays, others) = prepare.partition { it.dayOfWeek == DayOfWeek.MONDAY.value }
        assertTrue("registros de lunes: ${mondays.size}", mondays.size >= 3)
        val diff = mondays.map { it.actualMin }.average() - others.map { it.actualMin }.average()
        assertTrue("diferencia de los lunes: $diff", diff in 4.0..8.0)
    }

    @Test
    fun `la ultima semana coincide con el resumen del prototipo`() {
        val lastWeek = seed.sessions.filter { dateOf(it.startedAt) >= now.toLocalDate().minusDays(7) }
        assertEquals(6, lastWeek.size)
        assertEquals(5, lastWeek.count { (it.minutesLate ?: 0) <= 0 })
        val late = lastWeek.single { (it.minutesLate ?: 0) > 0 }
        assertEquals(DayOfWeek.THURSDAY, dateOf(late.startedAt).dayOfWeek)
        val best = lastWeek.minBy { it.minutesLate ?: 0 }
        assertEquals(DayOfWeek.WEDNESDAY, dateOf(best.startedAt).dayOfWeek)
    }
}
