package com.atiempo.wear.ui.prep

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.BottomAction
import com.atiempo.wear.ui.components.BottomActionButton
import com.atiempo.wear.ui.components.BottomFade
import com.atiempo.wear.ui.components.Countdown
import com.atiempo.wear.ui.components.DrawsOwnAmbient
import com.atiempo.wear.ui.components.FitColumn
import com.atiempo.wear.ui.components.TIME_TEXT_CLEARANCE
import com.atiempo.wear.ui.components.LocalAmbient
import com.atiempo.wear.ui.components.TintedIcon
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.components.clockText
import com.atiempo.wear.ui.components.icon
import com.atiempo.wear.ui.components.stepColor
import com.atiempo.wear.ui.components.title
import com.atiempo.wear.ui.theme.AmbientColors
import com.atiempo.wear.ui.theme.AppTheme
import com.atiempo.wear.ui.theme.TINT_ALPHA
import java.time.ZoneId

/** 3.6 Estados de preparación (`prep/{sessionId}`): conecta la pantalla con su ViewModel. */
@Composable
fun PrepRoute(container: AppContainer, sessionId: Long, onFinished: () -> Unit, onMissing: () -> Unit) {
    val viewModel: PrepViewModel = viewModel(
        key = "prep-$sessionId",
        factory = viewModelFactory { initializer { PrepViewModel(sessionId, container.prep, container.clock) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state) {
        PrepUiState.Finished -> LaunchedEffect(Unit) { onFinished() }
        PrepUiState.Missing -> LaunchedEffect(Unit) { onMissing() }
        else -> Unit
    }
    // La preparación tiene su propia versión en reposo (D-034): sigue a la vista con la muñeca abajo.
    DrawsOwnAmbient()
    val ambient = LocalAmbient.current.isAmbient
    PrepScreen(state, container.zone, onConfirm = viewModel::onConfirm, onToggleItem = viewModel::onToggleItem, ambient = ambient)
}

/**
 * Una sola pantalla que cambia según el estado activo (DISENO_UI §3.6): anillo completo en el color del
 * estado con el tiempo que queda, ícono, nombre, minutos y el botón "Listo" ("Ya salí" en el estado 4).
 * Si se acaba el tiempo de salir, el anillo y los textos pasan a `late`.
 */
@Composable
fun PrepScreen(
    state: PrepUiState,
    zone: ZoneId,
    onConfirm: () -> Unit,
    onToggleItem: (Int) -> Unit,
    ambient: Boolean = false,
) {
    if (ambient) {
        AmbientPrep(state as? PrepUiState.Active)
        return
    }
    Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        val active = state as? PrepUiState.Active ?: return@Box
        val color = stepColor(active.step, active.isLate)
        if (active.step == PrepStep.CHECK) {
            CheckContent(active, color, onToggleItem)
            BottomFade()
        } else {
            StandardContent(active, color, zone)
        }
        // El anillo va encima del contenido y de su degradado: siempre se ve completo. No es tocable.
        StepRing(active.step, if (active.isLate) 1f else active.remainingFraction, color)
        val leave = active.step == PrepStep.LEAVE
        BottomActionButton(
            label = stringResource(if (leave) R.string.action_left else R.string.action_done),
            onClick = onConfirm,
            icon = if (leave) R.drawable.ic_directions_walk else R.drawable.ic_check,
            color = color,
        )
    }
}

/**
 * Anillo del borde con el tiempo que queda del paso. Al cambiar de estado se dibuja de nuevo desde cero
 * con un barrido (DISENO_UI §3.6); después baja de forma continua.
 */
@Composable
private fun StepRing(step: PrepStep, target: Float, color: Color) {
    val progress = remember(step) { Animatable(0f) }
    LaunchedEffect(step, target) {
        val sweep = progress.value == 0f
        progress.animateTo(target, if (sweep) tween(900, easing = FastOutSlowInEasing) else tween(900, easing = LinearEasing))
    }
    CircularProgressIndicator(
        progress = { progress.value },
        modifier = Modifier.fillMaxSize().padding(2.dp),
        colors = ProgressIndicatorDefaults.colors(indicatorColor = color, trackColor = AppTheme.colors.surfaceBright),
        strokeWidth = 6.dp,
    )
}

/** Estados 1, 2 y 4: todo en un vistazo, sin desplazar. */
@Composable
private fun StandardContent(state: PrepUiState.Active, color: Color, zone: ZoneId) {
    val name = stringResource(state.step.title())
    val description = if (state.isLate) stringResource(R.string.a11y_prep_late, name, state.minutesLate)
    else stringResource(R.string.a11y_prep_left, name, state.minutesLeft)
    FitColumn(
        padding = PaddingValues(start = 26.dp * AppTheme.scale, end = 26.dp * AppTheme.scale, top = TIME_TEXT_CLEARANCE, bottom = BottomAction.Height + BottomAction.BottomGap),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        ) {
            // Ícono sin círculo para que todo quepa sobre el botón aunque el nombre ocupe dos líneas.
            Icon(painterResource(state.step.icon()), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(text = name, style = AppTheme.type.title, color = color, textAlign = TextAlign.Center, maxLines = 2)
            if (state.isLate) LateCountdown(state.minutesLate) else Countdown(minutes = state.minutesLeft)
        }
        when (state.step) {
            PrepStep.FINISH -> Message(stringResource(R.string.prep_msg_finish))
            PrepStep.PREPARE -> Message(stringResource(R.string.prep_msg_prepare))
            PrepStep.LEAVE -> {
                if (state.isLate) {
                    Text(
                        text = stringResource(R.string.prep_late, state.minutesLate),
                        style = AppTheme.type.label,
                        color = AppTheme.stateColors.late,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = stringResource(R.string.detail_arrive_at, clockText(state.arriveAt, zone)),
                    style = AppTheme.type.label,
                    color = AppTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            PrepStep.CHECK -> Unit
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(text = text, style = AppTheme.type.label, color = AppTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center)
}

/** "+3 min" en el color `late`. */
@Composable
private fun LateCountdown(minutes: Int) {
    Row {
        Text(text = "+$minutes", style = AppTheme.type.display, color = AppTheme.stateColors.late, modifier = Modifier.alignByBaseline())
        Text(
            text = stringResource(R.string.unit_min),
            style = AppTheme.type.displayUnit,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.alignByBaseline().padding(start = 3.dp),
        )
    }
}

/**
 * Estado 3: encabezado con el tiempo y la lista de objetos de la rutina como opciones marcables.
 * La lista se desplaza bajo el botón "Listo" (D-010) para mantener objetivos táctiles de 48 dp.
 */
@Composable
private fun CheckContent(state: PrepUiState.Active, color: Color, onToggleItem: (Int) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val direction = LocalLayoutDirection.current
    val name = stringResource(state.step.title())
    val description = stringResource(R.string.a11y_prep_left, name, state.minutesLeft)
    ScreenScaffold(scrollState = listState) { contentPadding ->
        val padding = PaddingValues(
            start = contentPadding.calculateStartPadding(direction),
            end = contentPadding.calculateEndPadding(direction),
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + BottomAction.ReservedHeight,
        )
        TransformingLazyColumn(state = listState, contentPadding = padding) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TintedIcon(icon = state.step.icon(), color = color, size = 34)
                    Text(text = name, style = AppTheme.type.title, color = color, textAlign = TextAlign.Center)
                    Countdown(minutes = state.minutesLeft)
                }
            }
            item {
                val all = state.checkedItems.size == state.items.size
                Text(
                    text = if (all) stringResource(R.string.prep_all_checked)
                    else stringResource(R.string.prep_checked, state.checkedItems.size, state.items.size),
                    style = AppTheme.type.label,
                    color = if (all) color else AppTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
            state.items.forEachIndexed { index, item ->
                item(key = "item-$index") {
                    val checked = index in state.checkedItems
                    val checkedLabel = stringResource(if (checked) R.string.a11y_checked else R.string.a11y_unchecked)
                    Button(
                        onClick = { onToggleItem(index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec)
                            .semantics { stateDescription = checkedLabel },
                        transformation = SurfaceTransformation(spec),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (checked) color.copy(alpha = TINT_ALPHA).compositeOver(AppTheme.colors.surface)
                            else AppTheme.colors.surface,
                            contentColor = AppTheme.colors.onSurface,
                        ),
                        border = BorderStroke(if (checked) 2.dp else 1.dp, if (checked) color else AppTheme.colors.surfaceBright),
                        label = { Text(text = item, style = AppTheme.type.button) },
                        secondaryLabel = null,
                        icon = { CheckDot(checked, color) },
                    )
                }
            }
        }
    }
}

/**
 * En reposo (D-034): fondo negro, anillo fino de contorno en blanco con lo que queda, nombre del paso y minutos.
 * Sin ícono relleno, sin colores de estado y sin botón; al levantar la muñeca vuelve la pantalla completa.
 */
@Composable
private fun AmbientPrep(state: PrepUiState.Active?) {
    Box(modifier = Modifier.fillMaxSize().background(AmbientColors.background), contentAlignment = Alignment.Center) {
        if (state == null) return@Box
        CircularProgressIndicator(
            progress = { if (state.isLate) 1f else state.remainingFraction },
            modifier = Modifier.fillMaxSize().padding(2.dp),
            colors = ProgressIndicatorDefaults.colors(indicatorColor = AmbientColors.onBackground, trackColor = AmbientColors.track),
            strokeWidth = 2.dp,
        )
        val name = stringResource(state.step.title())
        val description = if (state.isLate) stringResource(R.string.a11y_prep_late, name, state.minutesLate)
        else stringResource(R.string.a11y_prep_left, name, state.minutesLeft)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 30.dp).clearAndSetSemantics { contentDescription = description },
        ) {
            Text(text = name, style = AppTheme.type.title, color = AmbientColors.onBackground, textAlign = TextAlign.Center, maxLines = 2)
            if (state.isLate) {
                Text(
                    text = stringResource(R.string.prep_late, state.minutesLate),
                    style = AppTheme.type.title,
                    color = AmbientColors.onBackground,
                    textAlign = TextAlign.Center,
                )
            } else {
                Countdown(minutes = state.minutesLeft, color = AmbientColors.onBackground, unitColor = AmbientColors.onBackgroundVariant)
            }
        }
    }
}

/** Círculo de marcado: vacío o relleno con check. */
@Composable
private fun CheckDot(checked: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .then(
                if (checked) Modifier.background(color, CircleShape)
                else Modifier.border(2.dp, AppTheme.colors.onSurfaceVariant, CircleShape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = AppTheme.colors.onState, modifier = Modifier.size(16.dp))
        }
    }
}

// ---------- Previews ----------

private val previewZone: ZoneId = ZoneId.of("America/Bogota")
private fun preview(step: PrepStep, late: Boolean = false) = PrepUiState.Active(
    eventTitle = "Clase de cálculo",
    step = step,
    minutesLeft = 12,
    remainingFraction = 0.6f,
    isLate = late,
    minutesLate = if (late) 3 else 0,
    arriveAt = 1_794_833_880_000, // 7:58 en Bogotá
    items = listOf("Llaves", "Billetera", "Carné", "Cuaderno"),
    checkedItems = setOf(0),
)

@WearPreviews
@Composable
private fun PrepFinishLightPreview() = ATiempoPreview(dark = false) { PrepScreen(preview(PrepStep.FINISH), previewZone, {}, {}) }

@WearPreviews
@Composable
private fun PrepPrepareDarkPreview() = ATiempoPreview(dark = true) { PrepScreen(preview(PrepStep.PREPARE), previewZone, {}, {}) }

@WearPreviews
@Composable
private fun PrepCheckLightPreview() = ATiempoPreview(dark = false) { PrepScreen(preview(PrepStep.CHECK), previewZone, {}, {}) }

@WearPreviews
@Composable
private fun PrepLeaveLightPreview() = ATiempoPreview(dark = false) { PrepScreen(preview(PrepStep.LEAVE), previewZone, {}, {}) }

@WearPreviews
@Composable
private fun PrepLateDarkPreview() = ATiempoPreview(dark = true) { PrepScreen(preview(PrepStep.LEAVE, late = true), previewZone, {}, {}) }

@WearPreviews
@Composable
private fun PrepAmbientPreview() = ATiempoPreview(dark = false) { PrepScreen(preview(PrepStep.PREPARE), previewZone, {}, {}, ambient = true) }
