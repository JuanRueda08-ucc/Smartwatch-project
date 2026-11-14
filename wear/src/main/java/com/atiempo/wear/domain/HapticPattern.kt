package com.atiempo.wear.domain

import com.atiempo.wear.domain.model.PrepStep

/**
 * Patrones hápticos finales de DISENO_UI §4 (D-012). Tiempos en ms alternando pausa y vibración,
 * amplitudes 0–255, sin repetición. Se distinguen por ritmo, no solo por fuerza.
 */
enum class HapticPattern(val timings: LongArray, val amplitudes: IntArray) {
    FINISH(longArrayOf(0, 150), intArrayOf(0, 140)),
    PREPARE(longArrayOf(0, 150, 150, 150), intArrayOf(0, 180, 0, 180)),
    CHECK(longArrayOf(0, 90, 110, 90, 110, 90), intArrayOf(0, 210, 0, 210, 0, 210)),
    LEAVE(longArrayOf(0, 450, 200, 450), intArrayOf(0, 255, 0, 255)),
    LATE(longArrayOf(0, 450, 200, 450, 200, 450), intArrayOf(0, 255, 0, 255, 0, 255)),

    /** Clic breve. En el reloj se usa el efecto del sistema EFFECT_CLICK; este es el respaldo. */
    CONFIRM(longArrayOf(0, 40), intArrayOf(0, 160)),
    ;

    companion object {
        fun of(step: PrepStep): HapticPattern = when (step) {
            PrepStep.FINISH -> FINISH
            PrepStep.PREPARE -> PREPARE
            PrepStep.CHECK -> CHECK
            PrepStep.LEAVE -> LEAVE
        }
    }
}
