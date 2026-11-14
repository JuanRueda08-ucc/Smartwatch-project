package com.atiempo.wear.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoClockTest {
    /** Reloj del sistema falso que avanza a mano. */
    private class FakeWall(var time: Long = 1_000_000L) : Clock {
        override fun now() = time
    }

    private val wall = FakeWall()
    private val clock = DemoClock(base = wall)

    @Test
    fun `sin demo marca la hora real`() {
        wall.time += 60_000
        assertEquals(wall.time, clock.now())
        assertFalse(clock.accelerated)
    }

    @Test
    fun `en demo un minuto pasa en 5 segundos`() {
        clock.setAccelerated(true)
        val start = clock.now()
        wall.time += 5_000
        assertEquals(start + 60_000, clock.now())
    }

    @Test
    fun `al activar no salta la hora`() {
        wall.time += 30_000
        val before = clock.now()
        clock.setAccelerated(true)
        assertEquals(before, clock.now())
    }

    @Test
    fun `al desactivar vuelve a la hora real`() {
        clock.setAccelerated(true)
        wall.time += 10_000
        clock.setAccelerated(false)
        assertEquals(wall.time, clock.now())
        assertFalse(clock.accelerated)
    }

    @Test
    fun `saltar y seguir corriendo`() {
        clock.setAccelerated(true)
        clock.jumpTo(5_000_000L)
        assertEquals(5_000_000L, clock.now())
        wall.time += 1_000
        assertEquals(5_012_000L, clock.now())
        assertTrue(clock.accelerated)
    }

    @Test
    fun `convierte una hora de la demo a la hora real para las alarmas`() {
        clock.setAccelerated(true)
        val inOneDemoMinute = clock.now() + 60_000
        assertEquals(wall.time + 5_000, clock.toWallTime(inOneDemoMinute))
        // Comprobación de ida y vuelta.
        wall.time = clock.toWallTime(inOneDemoMinute)
        assertEquals(inOneDemoMinute, clock.now())
    }
}
