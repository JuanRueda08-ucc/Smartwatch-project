package com.atiempo.wear.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de color de DISENO_UI §2, con los mismos nombres en ambos temas.
 * Este es el único archivo de la app donde se escriben colores hexadecimales.
 */
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceBright: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val state1Finish: Color,
    val state2Prepare: Color,
    val state3Check: Color,
    val state4Leave: Color,
    val late: Color,
    val onState: Color,
)

/** Tema claro: el principal. Valores revisados en D-013 (contraste ≥ 4.5:1). */
val LightColors = AppColors(
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceBright = Color(0xFFE8EAF2),
    onSurface = Color(0xFF1A1B22),
    onSurfaceVariant = Color(0xFF5A5D6B),
    primary = Color(0xFF2F5BD3),
    state1Finish = Color(0xFF5B3FD1),
    state2Prepare = Color(0xFF0B6BB1),
    state3Check = Color(0xFF8A5A00),
    state4Leave = Color(0xFF0D7548),
    late = Color(0xFFC62828),
    onState = Color(0xFFFFFFFF),
)

val DarkColors = AppColors(
    background = Color(0xFF000000),
    surface = Color(0xFF1B1B1F),
    surfaceBright = Color(0xFF2A2A30),
    onSurface = Color(0xFFF2F2F5),
    onSurfaceVariant = Color(0xFFA8A8B3),
    primary = Color(0xFF8AB4FF),
    state1Finish = Color(0xFFB39DFF),
    state2Prepare = Color(0xFF5CC8FF),
    state3Check = Color(0xFFFFC857),
    state4Leave = Color(0xFF5EE6A0),
    late = Color(0xFFFF6B6B),
    onState = Color(0xFF000000),
)

/** Opacidad de los fondos suaves de tarjeta con el color de un estado (DISENO_UI §2). */
const val TINT_ALPHA = 0.12f
