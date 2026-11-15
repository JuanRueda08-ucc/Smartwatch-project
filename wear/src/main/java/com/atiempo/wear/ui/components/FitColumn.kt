package com.atiempo.wear.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import com.atiempo.wear.ui.theme.AppTheme
import kotlin.math.ceil
import kotlin.math.max

/**
 * Contenido de una pantalla fija (Inicio, estados, resultado), centrado entre `padding` (D-034):
 * 1. Si cabe, se ve tal cual.
 * 2. Si no cabe (pantalla pequeña o texto grande), se reduce hasta `minScale` para que todo se lea de un vistazo.
 * 3. Si aun así no cabe y `scrollable`, se desplaza con el dedo o la corona, con un degradado sobre el botón
 *    que indica que hay más. Inicio no se desplaza (subir abre Eventos): usa un `minScale` menor.
 */
@Composable
fun FitColumn(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    minScale: Float = if (scrollable) 0.82f else 0.6f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    val focus = remember { FocusRequester() }
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewport = constraints.maxHeight
        val scrolling = if (scrollable) {
            Modifier
                .rotaryScrollable(RotaryScrollableDefaults.behavior(scroll), focus)
                .requestFocusOnHierarchyActive()
                .verticalScroll(scroll)
        } else Modifier
        Layout(
            modifier = Modifier.fillMaxWidth().then(scrolling),
            content = {
                Column(
                    modifier = Modifier.padding(
                        start = padding.calculateStartPadding(direction),
                        end = padding.calculateEndPadding(direction),
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content,
                )
            },
        ) { measurables, constraints ->
            val top = padding.calculateTopPadding().roundToPx()
            val bottom = padding.calculateBottomPadding().roundToPx()
            val placeable = measurables.first().measure(Constraints(maxWidth = constraints.maxWidth))
            val available = (viewport - top - bottom).coerceAtLeast(1)
            val scale = if (placeable.height <= available) 1f else max(minScale, available.toFloat() / placeable.height)
            val scaledHeight = ceil(placeable.height * scale).toInt()
            val height = if (scrollable) max(viewport, top + scaledHeight + bottom) else viewport
            layout(constraints.maxWidth, height) {
                val y = top + ((available - scaledHeight) / 2).coerceAtLeast(0)
                placeable.placeWithLayer((constraints.maxWidth - placeable.width) / 2, y) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
            }
        }
        if (scrollable && scroll.canScrollForward) {
            val background = AppTheme.colors.background
            val height = padding.calculateBottomPadding() + FADE
            // Transparente arriba, opaco justo donde empieza la zona del botón: solo se funde la última franja.
            val solidFrom = FADE / height
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(height)
                    .background(Brush.verticalGradient(0f to Color.Transparent, solidFrom to background, 1f to background)),
            )
        }
    }
}

private val FADE = 16.dp

/** Espacio arriba para que el contenido no quede bajo la hora curva (también con texto grande). */
val TIME_TEXT_CLEARANCE = 24.dp
