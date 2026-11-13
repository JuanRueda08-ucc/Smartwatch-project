package com.atiempo.wear.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Shapes

/**
 * Formas de DISENO_UI §2: botones y chips en píldora (forma "full" de Wear, ya predeterminada)
 * y tarjetas con esquinas de 18 dp.
 */
val CardCorner = 18.dp

val AppShapes = Shapes(
    large = RoundedCornerShape(CardCorner),
)
