package com.atiempo.wear.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.Typography

/** Cifras tabulares para que los tiempos no "bailen" al cambiar. */
private const val TABULAR = "tnum"

/** Escala tipográfica de DISENO_UI §2. No se usan tamaños fuera de ella. */
@Immutable
data class AppType(
    val display: TextStyle = TextStyle(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR),
    /** Unidad que acompaña a `display`: "min", "h", "%". */
    val displayUnit: TextStyle = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    // Títulos y texto con corte de línea balanceado: en pantallas redondas evita palabras sueltas.
    val title: TextStyle = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, lineBreak = LineBreak.Heading),
    val body: TextStyle = TextStyle(fontSize = 15.sp, lineHeight = 19.sp, lineBreak = LineBreak.Heading),
    val button: TextStyle = TextStyle(fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
    val label: TextStyle = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    val caption: TextStyle = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
    /** Hora del TimeText curvo, en cifras tabulares. */
    val timeText: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR),
)

val LocalAppType = staticCompositionLocalOf { AppType() }

/**
 * La escala completa multiplicada por `factor` (tamaño y alto de línea). Se usa para que las pantallas
 * pequeñas conserven las proporciones de la grande (D-034). La hora del TimeText no cambia.
 */
fun AppType.scaled(factor: Float): AppType {
    if (factor == 1f) return this
    fun TextStyle.s() = copy(fontSize = fontSize * factor, lineHeight = lineHeight * factor)
    return copy(
        display = display.s(),
        displayUnit = displayUnit.s(),
        title = title.s(),
        body = body.s(),
        button = button.s(),
        label = label.s(),
        caption = caption.s(),
    )
}

/**
 * Asigna la escala propia a los slots de la tipografía de Wear Material 3, para que los
 * componentes estándar (botones, tarjetas, TimeText) ya usen nuestros tamaños.
 */
fun AppType.toWearTypography(): Typography = Typography(
    numeralMedium = display,
    displaySmall = title,
    titleLarge = title,
    titleMedium = title,
    titleSmall = button,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = label,
    bodyExtraSmall = caption,
    labelLarge = button,
    labelMedium = label,
    labelSmall = caption,
    arcMedium = CurvedTextStyle(timeText),
    arcSmall = CurvedTextStyle(timeText),
)
