package com.atiempo.wear.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.atiempo.wear.ui.theme.AppTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Forma en reposo: barras más altas al centro. */
private val IDLE_SHAPE = floatArrayOf(0.25f, 0.4f, 0.6f, 0.75f, 0.6f, 0.4f, 0.25f)

/**
 * Onda de 7 barras en `primary` (DISENO_UI §3.4). Mientras escucha, cada barra sube y baja con un
 * desfase distinto; en reposo queda quieta con forma de onda.
 */
@Composable
fun VoiceWave(active: Boolean, modifier: Modifier = Modifier, color: Color = AppTheme.colors.primary) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_100, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier = modifier.width(84.dp).height(48.dp)) {
        val bars = IDLE_SHAPE.size
        val barWidth = 7.dp.toPx()
        val gap = (size.width - bars * barWidth) / (bars - 1)
        IDLE_SHAPE.forEachIndexed { i, idle ->
            val factor = if (active) 0.2f + 0.8f * abs(sin(PI * (phase + i * 0.17))).toFloat() else idle
            val h = size.height * factor
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), (size.height - h) / 2),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }
    }
}
