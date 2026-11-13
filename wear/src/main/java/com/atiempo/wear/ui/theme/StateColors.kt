package com.atiempo.wear.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.atiempo.wear.domain.model.PrepStep

/**
 * Colores de los 4 estados. Van aparte del ColorScheme de Wear porque no tienen equivalente
 * en Material 3 (DISENO_UI §2, D-015).
 */
@Immutable
data class StateColors(
    val finish: Color,
    val prepare: Color,
    val check: Color,
    val leave: Color,
    val late: Color,
    val onState: Color,
) {
    fun of(step: PrepStep): Color = when (step) {
        PrepStep.FINISH -> finish
        PrepStep.PREPARE -> prepare
        PrepStep.CHECK -> check
        PrepStep.LEAVE -> leave
    }

    /** Fondo suave de tarjeta: el color del estado al 12 % sobre `surface`. */
    fun tint(color: Color, surface: Color): Color = color.copy(alpha = TINT_ALPHA).compositeOver(surface)
}

fun AppColors.toStateColors() = StateColors(
    finish = state1Finish,
    prepare = state2Prepare,
    check = state3Check,
    leave = state4Leave,
    late = late,
    onState = onState,
)

val LocalStateColors = staticCompositionLocalOf { LightColors.toStateColors() }
