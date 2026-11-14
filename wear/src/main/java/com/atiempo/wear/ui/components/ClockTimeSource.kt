package com.atiempo.wear.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.wear.compose.material3.TimeSource
import com.atiempo.wear.domain.time.Clock
import kotlinx.coroutines.delay
import java.time.ZoneId

/**
 * Hora del `TimeText` leída del reloj de la app, en el formato de la app ("7:05"). En modo demo muestra la
 * hora acelerada, igual que el resto de la app (D-029).
 */
class ClockTimeSource(private val clock: Clock, private val zone: ZoneId) : TimeSource {
    @Composable
    override fun currentTime(): String {
        val time by produceState(clockText(clock.now(), zone)) {
            while (true) {
                value = clockText(clock.now(), zone)
                delay(TICK_MS)
            }
        }
        return time
    }

    private companion object {
        // Cada medio segundo real: en modo demo un minuto pasa en 5 s.
        const val TICK_MS = 500L
    }
}
