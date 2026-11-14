package com.atiempo.wear.data.context

import com.atiempo.wear.domain.EstimationContext
import com.atiempo.wear.domain.model.TrafficLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeContextProviderTest {

    @Test
    fun `sin modo demo el dia es tranquilo aunque haya valores guardados`() = runTest {
        val provider = FakeContextProvider(MutableStateFlow(false), MutableStateFlow(true), MutableStateFlow(TrafficLevel.HIGH))
        assertEquals(EstimationContext(), provider.context.first())
    }

    @Test
    fun `con modo demo entrega la lluvia y el trafico elegidos`() = runTest {
        val provider = FakeContextProvider(MutableStateFlow(true), MutableStateFlow(true), MutableStateFlow(TrafficLevel.MEDIUM))
        assertEquals(EstimationContext(rain = true, traffic = TrafficLevel.MEDIUM), provider.context.first())
    }

    @Test
    fun `cambiar un valor cambia el contexto`() = runTest {
        val rain = MutableStateFlow(false)
        val provider = FakeContextProvider(MutableStateFlow(true), rain, MutableStateFlow(TrafficLevel.LOW))
        assertEquals(false, provider.context.first().rain)
        rain.value = true
        assertEquals(true, provider.context.first().rain)
    }

    @Test
    fun `el trafico da la vuelta bajo medio alto`() {
        assertEquals(TrafficLevel.MEDIUM, TrafficLevel.LOW.next())
        assertEquals(TrafficLevel.HIGH, TrafficLevel.MEDIUM.next())
        assertEquals(TrafficLevel.LOW, TrafficLevel.HIGH.next())
    }
}
