package com.atiempo.wear.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.atiempo.wear.R
import com.atiempo.wear.ui.theme.AppTheme

/**
 * Tiempo grande con su unidad: "12 min", desde una hora "1:05 h" y desde un día "3 d"
 * (DISENO_UI §2, `display`). El tiempo es el protagonista de cada pantalla.
 */
@Composable
fun Countdown(minutes: Int, modifier: Modifier = Modifier, color: Color = AppTheme.colors.onSurface) {
    val safe = minutes.coerceAtLeast(0)
    val (number, unit) = when {
        safe < 60 -> safe.toString() to stringResource(R.string.unit_min)
        safe < MINUTES_PER_DAY -> "%d:%02d".format(safe / 60, safe % 60) to stringResource(R.string.unit_h)
        // A más de un día, "71:58 h" no se lee de un vistazo: se muestran los días completos.
        else -> (safe / MINUTES_PER_DAY).toString() to stringResource(R.string.unit_days)
    }
    Row(modifier = modifier) {
        Text(text = number, style = AppTheme.type.display, color = color, modifier = Modifier.alignByBaseline())
        Text(
            text = unit,
            style = AppTheme.type.displayUnit,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.alignByBaseline().padding(start = 3.dp),
        )
    }
}

private const val MINUTES_PER_DAY = 24 * 60
