package com.atiempo.wear.data.context

import com.atiempo.wear.domain.EstimationContext
import com.atiempo.wear.domain.model.TrafficLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** De dónde sale el contexto del día (clima y tráfico) para el estimador. */
interface ContextProvider {
    val context: Flow<EstimationContext>
}

/**
 * Clima y tráfico simulados (D-003): no hay APIs reales. Los valores se eligen en Ajustes con el modo demo
 * activo y solo cuentan mientras el modo demo esté encendido; fuera de él el día es tranquilo (sin lluvia,
 * tráfico bajo), así un valor olvidado no mueve los avisos de verdad (D-031).
 */
class FakeContextProvider(
    demoMode: Flow<Boolean>,
    rain: Flow<Boolean>,
    traffic: Flow<TrafficLevel>,
) : ContextProvider {
    override val context: Flow<EstimationContext> = combine(demoMode, rain, traffic) { demo, isRaining, level ->
        resolve(demo, isRaining, level)
    }

    companion object {
        fun resolve(demo: Boolean, rain: Boolean, traffic: TrafficLevel): EstimationContext =
            if (demo) EstimationContext(rain = rain, traffic = traffic) else EstimationContext()
    }
}

/** Siguiente nivel al tocar el botón de tráfico: bajo → medio → alto → bajo. */
fun TrafficLevel.next(): TrafficLevel = TrafficLevel.entries[(ordinal + 1) % TrafficLevel.entries.size]
