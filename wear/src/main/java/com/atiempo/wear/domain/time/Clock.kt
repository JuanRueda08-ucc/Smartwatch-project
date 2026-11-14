package com.atiempo.wear.domain.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Fuente de la hora actual. Todas las pantallas la leen de aquí para que el modo demo (tarea 3.12)
 * pueda acelerar el tiempo sin tocar la lógica.
 */
fun interface Clock {
    /** Epoch en milisegundos. */
    fun now(): Long
}

object SystemClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
}

/** Emite la hora cada `periodMs`, empezando de inmediato. */
fun Clock.ticks(periodMs: Long = 1_000L): Flow<Long> = flow {
    while (true) {
        emit(now())
        delay(periodMs)
    }
}
