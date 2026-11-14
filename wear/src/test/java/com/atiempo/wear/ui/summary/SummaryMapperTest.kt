package com.atiempo.wear.ui.summary

import com.atiempo.wear.data.db.PrepSession
import com.atiempo.wear.data.seed.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SummaryMapperTest {
    private val zone = ZoneId.of("America/Bogota")
    private val today = LocalDate.of(2026, 11, 17) // martes
    private var nextId = 1L
    private fun session(daysAgo: Long, minutesLate: Int?, left: Boolean = true): PrepSession {
        val start = today.minusDays(daysAgo).atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        return PrepSession(nextId++, eventId = 1, startedAt = start, leftAt = if (left) start + 1_800_000 else null, minutesLate = minutesLate)
    }

    @Test
    fun `sin salidas todo queda vacio`() {
        val state = SummaryMapper.map(emptyList(), today, zone)
        assertEquals(7, state.days.size)
        assertTrue(state.days.all { it.result == DayResult.NONE })
        assertEquals(0, state.percent)
        assertNull(state.bestDay)
    }

    @Test
    fun `los 7 dias terminan hoy`() {
        val days = SummaryMapper.map(emptyList(), today, zone).days
        assertEquals(today.minusDays(6), days.first().date)
        assertEquals(today, days.last().date)
    }

    @Test
    fun `cuenta a tiempo, tarde y el mejor dia`() {
        val sessions = listOf(
            session(1, -2), session(2, 4), session(3, -8), session(4, -5), session(0, -3),
        )
        val state = SummaryMapper.map(sessions, today, zone)
        assertEquals(4, state.onTime)
        assertEquals(5, state.total)
        assertEquals(80, state.percent)
        assertEquals(DayOfWeek.SATURDAY, state.bestDay) // hace 3 días, con 8 min de margen
        assertEquals(DaySummary(today.minusDays(2), DayResult.LATE, 4), state.days[4])
    }

    @Test
    fun `no cuenta sesiones sin salir ni de hace mas de una semana`() {
        val state = SummaryMapper.map(listOf(session(1, null, left = false), session(7, -3), session(10, 5)), today, zone)
        assertEquals(0, state.total)
    }

    @Test
    fun `un dia con una salida tarde cuenta como dia tarde`() {
        val state = SummaryMapper.map(listOf(session(1, -5), session(1, 2)), today, zone)
        assertEquals(DayResult.LATE, state.days[5].result)
        assertEquals(1, state.onTime)
        assertEquals(2, state.total)
    }

    @Test
    fun `con los datos de ejemplo el resumen tiene contenido`() {
        val now = LocalDateTime.of(2026, 11, 17, 9, 0)
        val seed = SampleData.generate(now, zone)
        val state = SummaryMapper.map(seed.sessions, now.toLocalDate(), zone)
        assertTrue("salidas en la semana: ${state.total}", state.total >= 5)
        assertTrue(state.onTime in 1 until state.total)
    }
}
