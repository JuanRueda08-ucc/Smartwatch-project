package com.atiempo.wear.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Brush
import androidx.wear.compose.material3.SurfaceTransformation
import com.atiempo.wear.ui.theme.TINT_ALPHA
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.atiempo.wear.ui.theme.AppTheme

/*
 * Botones de la app con las medidas de DISENO_UI §2. Todos reciben colores explícitos del tema:
 * los predeterminados de Wear quedan en caché y no siguen el cambio de tema (D-015).
 */

/** Medidas del botón principal inferior (D-011). */
object BottomAction {
    val Width = 132.dp
    val Height = 48.dp
    val BottomGap = 26.dp
    /** Espacio que el contenido debe dejar libre abajo para no quedar bajo el botón. */
    val ReservedHeight = Height + BottomGap + 8.dp
}

/**
 * Botón principal en la parte inferior: 132 × 48 dp, a 26 dp del borde. Se coloca dentro de un Box
 * que ocupa la pantalla. `color` permite usar el color de un estado (el texto pasa a `onState`).
 */
@Composable
fun BoxScope.BottomActionButton(
    label: String,
    onClick: () -> Unit,
    @DrawableRes icon: Int? = null,
    color: Color = AppTheme.colors.primary,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = BottomAction.BottomGap)
            .width(BottomAction.Width)
            .height(BottomAction.Height),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = AppTheme.colors.onState,
            iconColor = AppTheme.colors.onState,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp),
        icon = icon?.let { { Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(20.dp)) } },
        label = {
            Text(
                text = label,
                style = AppTheme.type.button,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = if (icon == null) Modifier.fillMaxWidth() else Modifier,
            )
        },
    )
}

/**
 * Degradado al color de fondo detrás del botón inferior: el contenido que se desplaza se desvanece
 * antes de pasar bajo el botón (como en el prototipo).
 */
@Composable
fun BoxScope.BottomFade() {
    val background = AppTheme.colors.background
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(BottomAction.ReservedHeight + 16.dp)
            .background(Brush.verticalGradient(0f to background.copy(alpha = 0f), 0.45f to background)),
    )
}

/** Chip en píldora sobre `surface`, para acciones secundarias como "Ver plan". */
@Composable
fun ChipButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    transformation: SurfaceTransformation? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        transformation = transformation,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.onSurface,
            iconColor = AppTheme.colors.primary,
        ),
        border = BorderStroke(1.dp, AppTheme.colors.surfaceBright),
        contentPadding = PaddingValues(horizontal = 18.dp),
        icon = icon?.let { { Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(20.dp)) } },
        label = { Text(text = label, style = AppTheme.type.button, maxLines = 1) },
    )
}

/** Círculo tintado con un ícono, para ilustraciones simples (estado vacío, íconos de estado). */
@Composable
fun TintedIcon(@DrawableRes icon: Int, color: Color, size: Int = 56, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(color.copy(alpha = TINT_ALPHA).compositeOver(AppTheme.colors.surface), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size((size / 2).dp))
    }
}
