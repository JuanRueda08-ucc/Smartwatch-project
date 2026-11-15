package com.atiempo.wear.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/** Duración del cambio de color al cambiar de tema (DISENO_UI §3.12). */
private const val THEME_FADE_MS = 300

/**
 * Tema de la app. Wear Material 3 está pensado para fondo oscuro, así que ambos temas pasan un
 * ColorScheme completo construido desde nuestros tokens (D-008, D-015).
 */
@Composable
fun ATiempoTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = animatedColors(if (dark) DarkColors else LightColors)
    val scale = screenScale(LocalConfiguration.current.screenWidthDp)
    val type = remember(scale) { AppType().scaled(scale) }
    CompositionLocalProvider(
        LocalScreenScale provides scale,
        LocalAppColors provides colors,
        LocalStateColors provides colors.toStateColors(),
        LocalAppType provides type,
    ) {
        MaterialTheme(
            colorScheme = colors.toWearColorScheme(),
            typography = type.toWearTypography(),
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Ancho de referencia del diseño: Large Round (454 px a 320 dpi). */
const val REFERENCE_SCREEN_DP = 227f

/**
 * Cuánto se reduce el diseño en pantallas más pequeñas que la de referencia (Small Round: 192 dp → 0,85).
 * Nunca agranda; nunca baja de 0,8 para que el texto siga legible (D-034).
 */
fun screenScale(screenWidthDp: Int): Float = (screenWidthDp / REFERENCE_SCREEN_DP).coerceIn(0.8f, 1f)

val LocalScreenScale = staticCompositionLocalOf { 1f }

/** Acceso a los tokens propios desde cualquier pantalla. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
    val stateColors: StateColors
        @Composable @ReadOnlyComposable get() = LocalStateColors.current
    val type: AppType
        @Composable @ReadOnlyComposable get() = LocalAppType.current
    /** 1 en Large Round; menos en pantallas pequeñas. Para medidas fijas en dp que deben seguir la pantalla. */
    val scale: Float
        @Composable @ReadOnlyComposable get() = LocalScreenScale.current
}

/**
 * Correspondencia entre nuestros tokens y los roles de Wear Material 3 (D-015):
 * surface → surfaceContainer, surfaceBright → surfaceContainerHigh y outlineVariant,
 * onState → onPrimary (texto sobre botones rellenos), late → error.
 */
fun AppColors.toWearColorScheme() = ColorScheme(
    primary = primary,
    primaryDim = primary,
    primaryContainer = surfaceBright,
    onPrimary = onState,
    onPrimaryContainer = onSurface,
    secondary = primary,
    secondaryDim = primary,
    secondaryContainer = surfaceBright,
    onSecondary = onState,
    onSecondaryContainer = onSurface,
    tertiary = state1Finish,
    tertiaryDim = state1Finish,
    tertiaryContainer = surfaceBright,
    onTertiary = onState,
    onTertiaryContainer = onSurface,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceBright,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    outline = onSurfaceVariant,
    outlineVariant = surfaceBright,
    background = background,
    onBackground = onSurface,
    error = late,
    onError = onState,
    errorContainer = surfaceBright,
    onErrorContainer = late,
)

@Composable
private fun animatedColors(target: AppColors): AppColors {
    @Composable
    fun anim(c: Color, label: String) =
        animateColorAsState(c, animationSpec = tween(THEME_FADE_MS), label = label).value
    return AppColors(
        background = anim(target.background, "background"),
        surface = anim(target.surface, "surface"),
        surfaceBright = anim(target.surfaceBright, "surfaceBright"),
        onSurface = anim(target.onSurface, "onSurface"),
        onSurfaceVariant = anim(target.onSurfaceVariant, "onSurfaceVariant"),
        primary = anim(target.primary, "primary"),
        state1Finish = anim(target.state1Finish, "state1Finish"),
        state2Prepare = anim(target.state2Prepare, "state2Prepare"),
        state3Check = anim(target.state3Check, "state3Check"),
        state4Leave = anim(target.state4Leave, "state4Leave"),
        late = anim(target.late, "late"),
        onState = anim(target.onState, "onState"),
    )
}

val LocalAppColors = androidx.compose.runtime.staticCompositionLocalOf { LightColors }
