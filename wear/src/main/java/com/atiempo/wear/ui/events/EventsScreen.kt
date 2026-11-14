package com.atiempo.wear.ui.events

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.components.clockText
import com.atiempo.wear.ui.components.dayWithNumber
import com.atiempo.wear.ui.theme.AppTheme
import java.time.DayOfWeek
import java.time.ZoneId

/** 3.2 Lista de eventos (`events`): conecta la pantalla con su ViewModel. */
@Composable
fun EventsRoute(
    container: AppContainer,
    onNewEvent: () -> Unit,
    onOpenEvent: (eventId: Long) -> Unit,
    onRoutines: () -> Unit,
    onSummary: () -> Unit,
    onSettings: () -> Unit,
) {
    val viewModel: EventsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { EventsViewModel(container.events, container.routines, container.clock, container.zone) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    EventsScreen(state, container.zone, onNewEvent, onOpenEvent, onRoutines, onSummary, onSettings)
}

/**
 * Lista que se curva con el borde (TransformingLazyColumn): primero "Nuevo evento", luego los
 * eventos agrupados por día y al final los accesos a Rutinas, Resumen semanal y Ajustes.
 */
@Composable
fun EventsScreen(
    state: EventsUiState,
    zone: ZoneId,
    onNewEvent: () -> Unit,
    onOpenEvent: (eventId: Long) -> Unit,
    onRoutines: () -> Unit,
    onSummary: () -> Unit,
    onSettings: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) {
                    Text(text = stringResource(R.string.events_title), style = AppTheme.type.title, color = AppTheme.colors.onSurface)
                }
            }
            item {
                Button(
                    onClick = onNewEvent,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.primary,
                        contentColor = AppTheme.colors.onState,
                        iconColor = AppTheme.colors.onState,
                    ),
                    icon = { Icon(painterResource(R.drawable.ic_mic), contentDescription = null, modifier = Modifier.size(22.dp)) },
                    label = { Text(text = stringResource(R.string.action_new_event), style = AppTheme.type.button) },
                )
            }
            if (state is EventsUiState.Loaded) {
                if (state.groups.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.events_empty),
                            style = AppTheme.type.body,
                            color = AppTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        )
                    }
                }
                state.groups.forEach { group ->
                    item(key = "day-${group.day.dayOffset}") {
                        SectionLabel(dayHeader(group.day), spec)
                    }
                    group.events.forEach { event ->
                        item(key = "event-${event.id}") {
                            EventCard(event, zone, spec, onClick = { onOpenEvent(event.id) })
                        }
                    }
                }
            }
            item(key = "more") { SectionLabel(stringResource(R.string.events_more), spec) }
            item(key = "routines") { ShortcutButton(R.string.action_routines, R.drawable.ic_event_repeat, spec, onRoutines) }
            item(key = "summary") { ShortcutButton(R.string.action_summary, R.drawable.ic_bar_chart, spec, onSummary) }
            item(key = "settings") { ShortcutButton(R.string.action_settings, R.drawable.ic_settings, spec, onSettings) }
        }
    }
}

/** Encabezado de grupo: "Hoy", "Mañana" o "Jueves 19". */
@Composable
private fun dayHeader(day: DayLabel): String = dayWithNumber(day.dayOffset, day.weekday, day.dayOfMonth)

@Composable
private fun TransformingLazyColumnItemScope.SectionLabel(text: String, spec: TransformationSpec) {
    Text(
        text = text,
        style = AppTheme.type.label,
        color = AppTheme.colors.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
    )
}

/** Tarjeta de evento: la hora manda (DISENO_UI §1, "el tiempo es el protagonista"). */
@Composable
private fun TransformingLazyColumnItemScope.EventCard(
    event: EventItem,
    zone: ZoneId,
    spec: TransformationSpec,
    onClick: () -> Unit,
) {
    val leave = clockText(event.leaveAt, zone)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.onSurface,
        ),
        border = BorderStroke(1.dp, AppTheme.colors.surfaceBright),
    ) {
        Text(
            text = clockText(event.startAt, zone),
            style = AppTheme.type.title.copy(fontFeatureSettings = "tnum"),
            color = AppTheme.colors.onSurface,
        )
        Text(
            text = event.title,
            style = AppTheme.type.body,
            color = AppTheme.colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (event.place != null) stringResource(R.string.events_leave_at_place, leave, event.place)
            else stringResource(R.string.events_leave_at, leave),
            style = AppTheme.type.label,
            color = AppTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Acceso secundario al final de la lista, sobre `surface` con el ícono en `primary`. */
@Composable
private fun TransformingLazyColumnItemScope.ShortcutButton(
    @StringRes label: Int,
    @DrawableRes icon: Int,
    spec: TransformationSpec,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.onSurface,
            iconColor = AppTheme.colors.primary,
        ),
        border = BorderStroke(1.dp, AppTheme.colors.surfaceBright),
        icon = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp)) },
        label = { Text(text = stringResource(label), style = AppTheme.type.button) },
    )
}

// ---------- Previews ----------

private val previewZone: ZoneId = ZoneId.of("America/Bogota")
private const val MIN = 60_000L
private const val MONDAY_8AM = 1_794_834_000_000L // lunes 16 de noviembre de 2026, 8:00 en Bogotá
private val previewState = EventsUiState.Loaded(
    listOf(
        EventGroup(
            DayLabel(0, DayOfWeek.MONDAY, 16),
            listOf(
                EventItem(1, "Clase de cálculo", MONDAY_8AM, MONDAY_8AM - 17 * MIN, "Bloque 7"),
                EventItem(2, "Reunión de grupo", MONDAY_8AM + 360 * MIN, MONDAY_8AM + 330 * MIN, "Biblioteca"),
            ),
        ),
        EventGroup(
            DayLabel(3, DayOfWeek.THURSDAY, 19),
            listOf(EventItem(3, "Cita médica", MONDAY_8AM + (3 * 24 * 60 + 450) * MIN, MONDAY_8AM + (3 * 24 * 60 + 425) * MIN, "Clínica del Norte")),
        ),
    ),
)

@WearPreviews
@Composable
private fun EventsLightPreview() = ATiempoPreview(dark = false) {
    EventsScreen(previewState, previewZone, {}, {}, {}, {}, {})
}

@WearPreviews
@Composable
private fun EventsDarkPreview() = ATiempoPreview(dark = true) {
    EventsScreen(previewState, previewZone, {}, {}, {}, {}, {})
}

@WearPreviews
@Composable
private fun EventsEmptyLightPreview() = ATiempoPreview(dark = false) {
    EventsScreen(EventsUiState.Loaded(emptyList()), previewZone, {}, {}, {}, {}, {})
}
