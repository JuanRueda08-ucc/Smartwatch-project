package com.atiempo.wear.alerts

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.atiempo.wear.domain.HapticPattern

/**
 * Reproduce los patrones de DISENO_UI §4 con `VibrationEffect.createWaveform`.
 * En relojes sin control de amplitud se usa la fuerza predeterminada: los patrones igual se distinguen
 * por ritmo (D-012). Si la persona apagó las vibraciones en Ajustes, no suena nada.
 */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    /** Lo actualiza el AppContainer desde los Ajustes. */
    @Volatile
    var enabled: Boolean = true

    fun play(pattern: HapticPattern) {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        val effect = when {
            pattern == HapticPattern.CONFIRM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            v.hasAmplitudeControl() -> VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, NO_REPEAT)
            else -> VibrationEffect.createWaveform(pattern.timings, NO_REPEAT)
        }
        v.vibrate(effect)
    }

    private companion object {
        const val NO_REPEAT = -1
    }
}
