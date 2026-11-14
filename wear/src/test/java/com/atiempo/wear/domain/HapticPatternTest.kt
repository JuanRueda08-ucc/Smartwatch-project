package com.atiempo.wear.domain

import com.atiempo.wear.domain.model.PrepStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reglas de DISENO_UI §4: los patrones se reconocen sin mirar el reloj. */
class HapticPatternTest {

    private fun pulses(p: HapticPattern) = p.timings.filterIndexed { i, _ -> i % 2 == 1 }
    private fun gaps(p: HapticPattern) = p.timings.filterIndexed { i, _ -> i % 2 == 0 && i > 0 }

    @Test
    fun `tiempos y amplitudes tienen el mismo largo y amplitud valida`() {
        HapticPattern.entries.forEach {
            assertEquals(it.name, it.timings.size, it.amplitudes.size)
            assertTrue(it.name, it.amplitudes.all { a -> a in 0..255 })
        }
    }

    @Test
    fun `pausas de al menos 110 ms entre pulsos`() {
        HapticPattern.entries.forEach { p -> assertTrue(p.name, gaps(p).all { it >= 110 }) }
    }

    @Test
    fun `ningun aviso pasa de 2 segundos`() {
        HapticPattern.entries.forEach { assertTrue(it.name, it.timings.sum() <= 2_000) }
    }

    @Test
    fun `cada aviso de estado tiene un ritmo distinto`() {
        // Firma por cantidad de pulsos y si son largos (> 300 ms).
        val signatures = (PrepStep.entries.map { HapticPattern.of(it) } + HapticPattern.LATE)
            .map { p -> pulses(p).joinToString("") { if (it > 300) "L" else "c" } }
        assertEquals(listOf("c", "cc", "ccc", "LL", "LLL"), signatures)
    }

    @Test
    fun `la intensidad sube con la urgencia`() {
        val peaks = PrepStep.entries.map { HapticPattern.of(it).amplitudes.max() }
        assertEquals(peaks.sorted(), peaks)
    }
}
