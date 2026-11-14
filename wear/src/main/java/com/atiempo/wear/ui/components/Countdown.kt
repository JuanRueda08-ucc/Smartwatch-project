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
 * Tiempo grande con su unidad: "12 min" o, desde una hora, "1:05 h" (DISENO_UI §2, `display`).
 * El tiempo es el protagonista de cada pantalla.
 */
@Composable
fun Countdown(minutes: Int, modifier: Modifier = Modifier, color: Color = AppTheme.colors.onSurface) {
    val safe = minutes.coerceAtLeast(0)
    val (number, unit) = if (safe < 60) {
        safe.toString() to stringResource(R.string.unit_min)
    } else {
        "%d:%02d".format(safe / 60, safe % 60) to stringResource(R.string.unit_h)
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
