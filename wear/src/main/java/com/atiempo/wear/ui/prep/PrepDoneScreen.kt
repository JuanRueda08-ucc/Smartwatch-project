package com.atiempo.wear.ui.prep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.data.repo.EventRepository
import com.atiempo.wear.data.repo.HistoryRepository
import com.atiempo.wear.data.repo.RoutineRepository
import com.atiempo.wear.data.repo.stepDurations
import com.atiempo.wear.data.session.PrepSessionManager
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.BottomAction
import com.atiempo.wear.ui.components.BottomActionButton
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.components.clockText
import com.atiempo.wear.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.math.roundToInt

sealed interface ResultUiState {
    data object Loading : ResultUiState

    data class Loaded(
        /** Minutos después de la hora límite; 0 o menos = a tiempo. */
        val minutesLate: Int,
        val arriveAt: Long,
        /** Minutos que tomó prepararse: el paso "Prepárate" si se confirmó, si no toda la preparación. */
        val prepareMin: Int,
    ) : ResultUiState {
        val onTime: Boolean get() = minutesLate <= 0
    }
}

class PrepDoneViewModel(
    sessionId: Long,
    history: HistoryRepository,
    events: EventRepository,
    routines: RoutineRepository,
    manager: PrepSessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = history.session(sessionId) ?: return@launch
            val leftAt = session.leftAt ?: return@launch
            val event = events.get(session.eventId)
            val routine = event?.let { routines.get(it.routineId) }
            val travel = if (event != null && routine != null) routine.stepDurations(event).travel else 0
            val prepare = history.recordsFor(sessionId).firstOrNull { it.step == PrepStep.PREPARE }?.actualMin
                ?: ((leftAt - session.startedAt) / 60_000.0).roundToInt().coerceAtLeast(1)
            _state.value = ResultUiState.Loaded(
                minutesLate = session.minutesLate ?: 0,
                arriveAt = leftAt + travel * 60_000L,
                prepareMin = prepare,
            )
            // El resultado ya quedó leído: la sesión se libera para el próximo evento.
            manager.dismissFinished(sessionId)
        }
    }
}

/** 3.7 Resultado (`prep/{sessionId}/done`). */
@Composable
fun PrepDoneRoute(container: AppContainer, sessionId: Long, onDone: () -> Unit) {
    val viewModel: PrepDoneViewModel = viewModel(
        key = "done-$sessionId",
        factory = viewModelFactory {
            initializer { PrepDoneViewModel(sessionId, container.history, container.events, container.routines, container.prep) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    PrepDoneScreen(state, container.zone, onDone)
}

/**
 * "Saliste a tiempo" con check verde, o "Saliste 4 min tarde" sin regaños (DISENO_UI §3.7 y §5),
 * y una línea de aprendizaje.
 */
@Composable
fun PrepDoneScreen(state: ResultUiState, zone: ZoneId, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        val result = state as? ResultUiState.Loaded ?: return@Box
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp)
                .padding(top = 20.dp, bottom = BottomAction.Height + BottomAction.BottomGap + 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (result.onTime) AppTheme.stateColors.leave else AppTheme.colors.surfaceBright, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (result.onTime) R.drawable.ic_check else R.drawable.ic_schedule),
                    contentDescription = null,
                    tint = if (result.onTime) AppTheme.colors.onState else AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (result.onTime) stringResource(R.string.result_on_time)
                else stringResource(R.string.result_late, result.minutesLate),
                style = AppTheme.type.title,
                color = AppTheme.colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.detail_arrive_at, clockText(result.arriveAt, zone)),
                style = AppTheme.type.label,
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (result.onTime) stringResource(R.string.result_learn_on_time, result.prepareMin)
                else stringResource(R.string.result_learn_late, result.prepareMin, result.minutesLate),
                style = AppTheme.type.caption,
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        BottomActionButton(label = stringResource(R.string.action_done), onClick = onDone, icon = R.drawable.ic_check)
    }
}

// ---------- Previews ----------

private val previewZone: ZoneId = ZoneId.of("America/Bogota")

@WearPreviews
@Composable
private fun ResultOnTimeLightPreview() = ATiempoPreview(dark = false) {
    PrepDoneScreen(ResultUiState.Loaded(minutesLate = -2, arriveAt = 1_794_833_880_000, prepareMin = 22), previewZone) {}
}

@WearPreviews
@Composable
private fun ResultOnTimeDarkPreview() = ATiempoPreview(dark = true) {
    PrepDoneScreen(ResultUiState.Loaded(minutesLate = -2, arriveAt = 1_794_833_880_000, prepareMin = 22), previewZone) {}
}

@WearPreviews
@Composable
private fun ResultLateLightPreview() = ATiempoPreview(dark = false) {
    PrepDoneScreen(ResultUiState.Loaded(minutesLate = 4, arriveAt = 1_794_834_240_000, prepareMin = 27), previewZone) {}
}
