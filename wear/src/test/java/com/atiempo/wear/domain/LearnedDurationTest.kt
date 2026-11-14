package com.atiempo.wear.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LearnedDurationTest {

    @Test
    fun `sin historial no hay base aprendida`() {
        assertNull(LearnedDuration.ema(emptyList()))
        assertEquals(15, LearnedDuration.minutesOr(default = 15, minutes = emptyList()))
    }

    @Test
    fun `con historial constante la base converge a ese valor`() {
        assertEquals(20.0, LearnedDuration.ema(List(10) { 20 })!!, 1e-9)
    }

    @Test
    fun `lo reciente pesa mas`() {
        // 10 → 0,3·20 + 0,7·10 = 13
        assertEquals(13.0, LearnedDuration.ema(listOf(10, 20))!!, 1e-9)
        // Tras subir a 30 varias veces, la base se acerca a 30 pero no llega de golpe.
        val ema = LearnedDuration.ema(listOf(10, 30, 30, 30))!!
        assertEquals(23.14, ema, 0.01)
    }

    @Test
    fun `se redondea a minutos enteros`() {
        assertEquals(13, LearnedDuration.minutesOr(default = 5, minutes = listOf(10, 20)))
    }
}
