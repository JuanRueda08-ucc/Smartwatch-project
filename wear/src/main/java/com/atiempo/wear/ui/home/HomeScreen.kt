package com.atiempo.wear.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.BottomAction
import com.atiempo.wear.ui.components.BottomActionButton
import com.atiempo.wear.ui.components.ChipButton
import com.atiempo.wear.ui.components.Countdown
import com.atiempo.wear.ui.components.TintedIcon
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.components.eventWhen
import com.atiempo.wear.ui.theme.AppTheme
import java.time.DayOfWeek
import java.time.ZoneId

/** 3.1 Inicio (`home`): conecta la pantalla con su ViewModel. */
@Composable
fun HomeRoute(
    container: AppContainer,
    onViewPlan: (eventId: Long) -> Unit,
    onViewEvents: () -> Unit,
    onCreateByVoice: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(container.events, container.routines, container.clock, container.zone) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state, container.zone, onViewPlan, onViewEvents, onCreateByVoice)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    zone: ZoneId,
    onViewPlan: (eventId: Long) -> Unit,
    onViewEvents: () -> Unit,
    onCreateByVoice: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .swipeUpOrRotate(onViewEvents),
    ) {
        when (state) {
            HomeUiState.Loading -> Unit
            HomeUiState.Empty -> EmptyHome(onCreateByVoice)
            is HomeUiState.Upcoming -> UpcomingHome(state, zone, onViewPlan, onViewEvents)
        }
    }
}

@Composable
private fun UpcomingHome(
    state: HomeUiState.Upcoming,
    zone: ZoneId,
    onViewPlan: (Long) -> Unit,
    onViewEvents: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Anillo exterior: se llena durante la última hora antes de empezar a prepararse.
        CircularProgressIndicator(
            progress = { state.ringProgress },
            modifier = Modifier.fillMaxSize().padding(2.dp),
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = AppTheme.colors.primary,
                trackColor = AppTheme.colors.surfaceBright,
            ),
            strokeWidth = 5.dp,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = safeHorizontalPadding(), vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Text(
                text = eventWhen(state.dayOffset, state.weekday, state.eventAt, zone),
                style = AppTheme.type.label,
                color = AppTheme.colors.onSurfaceVariant,
            )
            Text(
                text = state.title,
                style = AppTheme.type.title,
                color = AppTheme.colors.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            if (state.isDue) {
                Text(
                    text = stringResource(R.string.home_time_to_prepare),
                    style = AppTheme.type.body,
                    color = AppTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(text = stringResource(R.string.home_now), style = AppTheme.type.display, color = AppTheme.colors.primary)
            } else {
                val description = pluralStringResource(R.plurals.a11y_prepare_in, state.minutesUntilPrep, state.minutesUntilPrep)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                ) {
                    Text(
                        text = stringResource(R.string.home_prepare_in),
                        style = AppTheme.type.body,
                        color = AppTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Countdown(minutes = state.minutesUntilPrep)
                }
            }
            Spacer(Modifier.height(6.dp))
            ChipButton(label = stringResource(R.string.action_view_plan), onClick = { onViewPlan(state.eventId) })
        }
        // Flecha hacia arriba: pista visual del gesto que abre la lista de eventos.
        IconButton(
            onClick = onViewEvents,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = AppTheme.colors.onSurfaceVariant),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_up),
                contentDescription = stringResource(R.string.action_view_events),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun EmptyHome(onCreateByVoice: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // La ilustración es el micrófono: invita a dictar el evento. El botón va sin ícono porque
        // "Crear con voz" con ícono no cabe en 132 dp (D-018).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = safeHorizontalPadding())
                .padding(top = 28.dp, bottom = BottomAction.ReservedHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            TintedIcon(icon = R.drawable.ic_mic, color = AppTheme.colors.primary, size = 44)
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_empty_title),
                style = AppTheme.type.title,
                color = AppTheme.colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.home_empty_body),
                style = AppTheme.type.body,
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        BottomActionButton(label = stringResource(R.string.home_create_voice), onClick = onCreateByVoice)
    }
}

/** Margen seguro del 10 % a cada lado en pantallas redondas (DISENO_UI §1). */
@Composable
private fun safeHorizontalPadding() = (LocalConfiguration.current.screenWidthDp * 0.1f).dp

/**
 * Deslizar hacia arriba o girar la corona hacia abajo abre la lista de eventos (DISENO_UI §3.1).
 */
@Composable
private fun Modifier.swipeUpOrRotate(onTrigger: () -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    return this
        .pointerInput(onTrigger) {
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = { if (total < -threshold) onTrigger() },
                onVerticalDrag = { _, dy -> total += dy },
            )
        }
        .onRotaryScrollEvent { event ->
            if (event.verticalScrollPixels > 0) onTrigger()
            true
        }
        // Dentro de SwipeDismissableNavHost solo la pantalla activa puede tomar el foco de la corona.
        .requestFocusOnHierarchyActive()
        .focusable()
}

// ---------- Previews ----------

private val previewZone: ZoneId = ZoneId.of("America/Bogota")
private val previewUpcoming = HomeUiState.Upcoming(
    eventId = 1,
    title = "Clase de cálculo",
    eventAt = 1_794_834_000_000, // lunes 16 de noviembre de 2026, 8:00 en Bogotá
    dayOffset = 0,
    weekday = DayOfWeek.MONDAY,
    minutesUntilPrep = 7,
    ringProgress = 0.88f,
)

@WearPreviews
@Composable
private fun HomeUpcomingLightPreview() = ATiempoPreview(dark = false) {
    HomeScreen(previewUpcoming, previewZone, {}, {}, {})
}

@WearPreviews
@Composable
private fun HomeUpcomingDarkPreview() = ATiempoPreview(dark = true) {
    HomeScreen(previewUpcoming, previewZone, {}, {}, {})
}

@WearPreviews
@Composable
private fun HomeDueLightPreview() = ATiempoPreview(dark = false) {
    HomeScreen(previewUpcoming.copy(minutesUntilPrep = 0, ringProgress = 1f), previewZone, {}, {}, {})
}

@WearPreviews
@Composable
private fun HomeEmptyLightPreview() = ATiempoPreview(dark = false) {
    HomeScreen(HomeUiState.Empty, previewZone, {}, {}, {})
}

@WearPreviews
@Composable
private fun HomeEmptyDarkPreview() = ATiempoPreview(dark = true) {
    HomeScreen(HomeUiState.Empty, previewZone, {}, {}, {})
}
