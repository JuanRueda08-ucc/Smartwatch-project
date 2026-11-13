package com.atiempo.wear.domain

import com.atiempo.wear.domain.model.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolverTest {

    @Test
    fun `claro y oscuro no dependen de la hora`() {
        for (hour in 0..23) {
            assertFalse(ThemeResolver.isDark(ThemeMode.LIGHT, hour))
            assertTrue(ThemeResolver.isDark(ThemeMode.DARK, hour))
        }
    }

    @Test
    fun `automatico es claro de 6 a 18 y oscuro de 19 a 5`() {
        val darkHours = (0..23).filter { ThemeResolver.isDark(ThemeMode.AUTO, it) }
        assertTrue(darkHours == listOf(0, 1, 2, 3, 4, 5, 19, 20, 21, 22, 23))
    }

    @Test
    fun `los bordes del modo automatico`() {
        assertTrue(ThemeResolver.isDark(ThemeMode.AUTO, 5))
        assertFalse(ThemeResolver.isDark(ThemeMode.AUTO, 6))
        assertFalse(ThemeResolver.isDark(ThemeMode.AUTO, 18))
        assertTrue(ThemeResolver.isDark(ThemeMode.AUTO, 19))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza horas imposibles`() {
        ThemeResolver.isDark(ThemeMode.AUTO, 24)
    }
}
