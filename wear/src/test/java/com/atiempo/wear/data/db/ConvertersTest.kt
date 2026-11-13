package com.atiempo.wear.data.db

import com.atiempo.wear.domain.model.PrepStep
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `la lista de objetos va y vuelve igual`() {
        val items = listOf("Llaves", "Billetera", "Carné", "Orden médica, copia")
        assertEquals(items, converters.toItems(converters.fromItems(items)))
    }

    @Test
    fun `la lista vacia se conserva vacia`() {
        assertEquals(emptyList<String>(), converters.toItems(converters.fromItems(emptyList())))
    }

    @Test
    fun `un solo objeto no se parte`() {
        assertEquals(listOf("Llaves"), converters.toItems(converters.fromItems(listOf("Llaves"))))
    }

    @Test
    fun `cada paso va y vuelve igual`() {
        PrepStep.entries.forEach { assertEquals(it, converters.toStep(converters.fromStep(it))) }
    }
}
