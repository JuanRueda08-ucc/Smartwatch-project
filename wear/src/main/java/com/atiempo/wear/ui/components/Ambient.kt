package com.atiempo.wear.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.wear.compose.material3.Text
import com.atiempo.wear.ui.theme.AmbientColors
import com.atiempo.wear.ui.theme.AppTheme

/**
 * Estado del modo ambiente (reloj en reposo, D-034). Lo actualiza `MainActivity` con el
 * `AmbientLifecycleObserver`; las pantallas lo leen con [LocalAmbient].
 */
@Stable
class AmbientState {
    /** true mientras el reloj está en reposo. */
    var isAmbient by mutableStateOf(false)

    /** Sube cada minuto en reposo (`onUpdateAmbient`), para redibujar la hora y la cuenta regresiva. */
    var minuteTicks by mutableIntStateOf(0)

    /** Pantallas que saben dibujarse en reposo (hoy, solo la preparación). Si es 0, se tapa con la hora. */
    var ambientAwareScreens by mutableIntStateOf(0)
}

val LocalAmbient = compositionLocalOf { AmbientState() }

/** La pantalla que lo llama se dibuja ella misma en reposo; las demás quedan tapadas por [AmbientClock]. */
@Composable
fun DrawsOwnAmbient() {
    val ambient = LocalAmbient.current
    DisposableEffect(ambient) {
        ambient.ambientAwareScreens++
        onDispose { ambient.ambientAwareScreens-- }
    }
}

/**
 * En reposo, sobre las pantallas que no tienen versión ambiente: fondo negro y la hora en blanco, sin
 * colores ni rellenos (cuida la batería y evita marcas en la pantalla).
 */
@Composable
fun AmbientClock(time: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(AmbientColors.background).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = time, style = AppTheme.type.display, color = AmbientColors.onBackground)
    }
}
