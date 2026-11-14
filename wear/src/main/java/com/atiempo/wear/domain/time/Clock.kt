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

    /**
     * La hora real del sistema en que este reloj marcará `time`. Las alarmas del sistema se programan con
     * ella. En un reloj normal es la misma hora.
     */
    fun toWallTime(time: Long): Long = time
}

object SystemClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
}

/**
 * Reloj del modo demo (D-029): con `accelerated` activo avanza `factor` veces más rápido (12: 1 min en 5 s)
 * desde el momento en que se activó; al desactivarlo vuelve a la hora real. `jumpTo` salta a una hora
 * (por ejemplo, unos segundos antes del próximo aviso) y sigue corriendo desde ahí.
 */
class DemoClock(private val base: Clock = SystemClock, private val factor: Int = DEFAULT_FACTOR) : Clock {

    @Volatile private var anchorWall = base.now()
    @Volatile private var anchorTime = anchorWall
    @Volatile var accelerated: Boolean = false
        private set

    private val speed: Int get() = if (accelerated) factor else 1

    override fun now(): Long = anchorTime + (base.now() - anchorWall) * speed

    override fun toWallTime(time: Long): Long = anchorWall + (time - anchorTime) / speed

    /** Activar acelera desde la hora que marca ahora; desactivar vuelve a la hora real. */
    @Synchronized
    fun setAccelerated(enabled: Boolean) {
        if (enabled == accelerated) return
        val wall = base.now()
        anchorTime = if (enabled) now() else wall
        anchorWall = wall
        accelerated = enabled
    }

    /** Salta a `time` y sigue corriendo desde ahí, a la velocidad actual. */
    @Synchronized
    fun jumpTo(time: Long) {
        anchorWall = base.now()
        anchorTime = time
    }

    companion object {
        /** 1 min de la demo pasa en 5 s reales. */
        const val DEFAULT_FACTOR = 12
    }
}

/** Emite la hora cada `periodMs` reales, empezando de inmediato. */
fun Clock.ticks(periodMs: Long = 1_000L): Flow<Long> = flow {
    while (true) {
        emit(now())
        delay(periodMs)
    }
}
