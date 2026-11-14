package com.atiempo.wear.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.ChipButton
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.components.clockText
import com.atiempo.wear.ui.components.dayName
import com.atiempo.wear.ui.theme.AppTheme
import com.atiempo.wear.ui.theme.TINT_ALPHA
import java.time.DayOfWeek
import java.time.ZoneId

/** 3.3 Detalle del evento (`event/{eventId}`): conecta la pantalla con su ViewModel. */
@Composable
fun EventDetailRoute(
    container: AppContainer,
    eventId: Long,
    onWhyThisTime: (eventId: Long) -> Unit,
    onStartNow: (eventId: Long) -> Unit,
) {
    val viewModel: EventDetailViewModel = viewModel(
        key = "event-$eventId",
        factory = viewModelFactory {
            initializer { EventDetailViewModel(eventId, container.events, container.routines, container.clock, container.zone) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    EventDetailScreen(state, container.zone, onWhyThisTime, onStartNow)
}

@Composable
fun EventDetailScreen(
    state: EventDetailUiState,
    zone: ZoneId,
    onWhyThisTime: (eventId: Long) -> Unit,
    onStartNow: (eventId: Long) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        when (state) {
            EventDetailUiState.Loading -> Unit
            EventDetailUiState.NotFound -> Text(
                text = stringResource(R.string.detail_not_found),
                style = AppTheme.type.body,
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            )
            is EventDetailUiState.Loaded -> LoadedDetail(state, zone, onWhyThisTime, onStartNow)
        }
    }
}

@Composable
private fun LoadedDetail(
    state: EventDetailUiState.Loaded,
    zone: ZoneId,
    onWhyThisTime: (Long) -> Unit,
    onStartNow: (Long) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
                item {
                    ListHeader(
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    ) {
                        Text(
                            text = state.title,
                            style = AppTheme.type.title,
                            color = AppTheme.colors.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                item {
                    val day = dayName(state.dayOffset, state.weekday)
                    val time = clockText(state.eventAt, zone)
                    Text(
                        text = if (state.place != null) stringResource(R.string.event_when_place, day, time, state.place)
                        else stringResource(R.string.event_when, day, time),
                        style = AppTheme.type.label,
                        color = AppTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    )
                }
                item {
                    Timeline(state.steps, zone, Modifier.fillMaxWidth().padding(horizontal = 4.dp))
                }
                item {
                    Text(
                        text = stringResource(R.string.detail_arrive_at, clockText(state.arriveAt, zone)),
                        style = AppTheme.type.label,
                        color = AppTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                    )
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), contentAlignment = Alignment.Center) {
                        ChipButton(
                            label = stringResource(R.string.action_why_this_time),
                            onClick = { onWhyThisTime(state.eventId) },
                            transformation = SurfaceTransformation(spec),
                        )
                    }
                }
                if (state.canStart) {
                    // Acción secundaria: normalmente se espera al aviso. Va al final para que el primer
                    // vistazo muestre la línea de tiempo y no un botón fijo (D-019).
                    item {
                        Button(
                            onClick = { onStartNow(state.eventId) },
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, spec).padding(top = 6.dp),
                            transformation = SurfaceTransformation(spec),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.primary,
                                contentColor = AppTheme.colors.onState,
                            ),
                            label = {
                                Text(
                                    text = stringResource(R.string.action_start_now),
                                    style = AppTheme.type.button,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Línea de tiempo vertical de los 4 estados, cada punto con el color de su estado (DISENO_UI §3.3).
 * El paso que toca ahora va sobre su color al 12 % con borde; los pasados llevan check y texto atenuado.
 */
@Composable
private fun Timeline(steps: List<TimelineStep>, zone: ZoneId, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        steps.forEachIndexed { index, step ->
            TimelineRow(step, zone, isFirst = index == 0, isLast = index == steps.lastIndex)
        }
    }
}

@Composable
private fun TimelineRow(step: TimelineStep, zone: ZoneId, isFirst: Boolean, isLast: Boolean) {
    val color = AppTheme.stateColors.of(step.step)
    val connector = AppTheme.colors.surfaceBright
    val past = step.status == StepStatus.PAST
    val current = step.status == StepStatus.CURRENT
    val name = stepName(step.step)
    val time = clockText(step.startAt, zone)
    val description = when (step.status) {
        StepStatus.CURRENT -> stringResource(R.string.a11y_step_current, time, name)
        StepStatus.PAST -> stringResource(R.string.a11y_step_past, time, name)
        StepStatus.UPCOMING -> stringResource(R.string.a11y_step_upcoming, time, name)
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Altura intrínseca: la columna del punto ocupa toda la fila y el conector no se corta.
            .height(IntrinsicSize.Min)
            .heightIn(min = 40.dp)
            .then(
                if (current) {
                    Modifier
                        .background(color.copy(alpha = TINT_ALPHA).compositeOver(AppTheme.colors.surface), shape)
                        .border(1.dp, color, shape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 10.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Columna del punto, con el conector hacia el paso anterior y el siguiente.
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val stroke = 2.dp.toPx()
                    if (!isFirst) drawLine(connector, Offset(x, 0f), Offset(x, size.height / 2), stroke)
                    if (!isLast) drawLine(connector, Offset(x, size.height / 2), Offset(x, size.height), stroke)
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(16.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                if (past) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = AppTheme.colors.onState,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = time,
            style = AppTheme.type.title.copy(fontFeatureSettings = "tnum"),
            color = if (past) AppTheme.colors.onSurfaceVariant else AppTheme.colors.onSurface,
            modifier = Modifier.width(62.dp),
        )
        Text(
            text = name,
            style = AppTheme.type.label,
            color = if (past) AppTheme.colors.onSurfaceVariant else AppTheme.colors.onSurface,
            maxLines = 2,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun stepName(step: PrepStep): String = stringResource(
    when (step) {
        PrepStep.FINISH -> R.string.step_finish
        PrepStep.PREPARE -> R.string.step_prepare
        PrepStep.CHECK -> R.string.step_check
        PrepStep.LEAVE -> R.string.step_leave_short
    },
)

// ---------- Previews ----------

private val previewZone: ZoneId = ZoneId.of("America/Bogota")
private const val MIN = 60_000L
private const val EVENT_AT = 1_794_834_000_000L // lunes 16 de noviembre de 2026, 8:00 en Bogotá

private fun previewState(current: Int) = EventDetailUiState.Loaded(
    eventId = 1,
    title = "Clase de cálculo",
    eventAt = EVENT_AT,
    dayOffset = 0,
    weekday = DayOfWeek.MONDAY,
    place = "Bloque 7",
    steps = listOf(47, 37, 22, 17).mapIndexed { i, before ->
        TimelineStep(
            step = PrepStep.entries[i],
            startAt = EVENT_AT - before * MIN,
            status = when {
                current < 0 || i > current -> StepStatus.UPCOMING
                i == current -> StepStatus.CURRENT
                else -> StepStatus.PAST
            },
        )
    },
    arriveAt = EVENT_AT - 5 * MIN,
    canStart = true,
)

@WearPreviews
@Composable
private fun EventDetailLightPreview() = ATiempoPreview(dark = false) {
    EventDetailScreen(previewState(current = -1), previewZone, {}, {})
}

@WearPreviews
@Composable
private fun EventDetailDarkPreview() = ATiempoPreview(dark = true) {
    EventDetailScreen(previewState(current = -1), previewZone, {}, {})
}

@WearPreviews
@Composable
private fun EventDetailInProgressLightPreview() = ATiempoPreview(dark = false) {
    EventDetailScreen(previewState(current = 1), previewZone, {}, {})
}
