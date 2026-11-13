package com.atiempo.wear.domain

import com.atiempo.wear.domain.model.ThemeMode

/**
 * Decide si se usa el tema oscuro. En modo automático el claro va de 6:00 a 18:59
 * y el oscuro de 19:00 a 5:59 (DISENO_UI §2).
 */
object ThemeResolver {
    const val LIGHT_FROM_HOUR = 6
    const val DARK_FROM_HOUR = 19

    fun isDark(mode: ThemeMode, hour: Int): Boolean {
        require(hour in 0..23) { "Hora fuera de rango: $hour" }
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.AUTO -> hour < LIGHT_FROM_HOUR || hour >= DARK_FROM_HOUR
        }
    }
}
