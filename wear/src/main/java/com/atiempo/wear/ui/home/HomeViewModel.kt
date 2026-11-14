package com.atiempo.wear.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atiempo.wear.data.repo.EstimationData
import com.atiempo.wear.data.repo.EventRepository
import com.atiempo.wear.data.repo.HistoryRepository
import com.atiempo.wear.data.repo.RoutineRepository
import com.atiempo.wear.data.session.ActiveSession
import com.atiempo.wear.data.session.PrepSessionManager
import com.atiempo.wear.domain.PrepPhase
import com.atiempo.wear.domain.time.Clock
import com.atiempo.wear.domain.time.ticks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

class HomeViewModel(
    events: EventRepository,
    routines: RoutineRepository,
    clock: Clock,
    zone: ZoneId,
    prep: PrepSessionManager,
    history: HistoryRepository,
    estimation: Flow<EstimationData>,
) : ViewModel() {

    // Lo que cambia sin el reloj: eventos, rutinas y para cuáles ya se salió.
    private val data = combine(
        events.upcoming(from = clock.now() - DAY_MS),
        routines.all(),
        history.leftEventIds(),
        estimation,
    ) { e, r, left, est -> Inputs(e, r, left, est) }

    private data class Inputs(
        val events: List<com.atiempo.wear.data.db.Event>,
        val routines: List<com.atiempo.wear.data.db.Routine>,
        val left: Set<Long>,
        val estimation: EstimationData,
    )

    val state: StateFlow<HomeUiState> = combine(data, clock.ticks(), prep.active) { inputs, now, session ->
        session?.takeIf { it.state.phase == PrepPhase.ACTIVE }?.let { active(it, now) }
            ?: HomeStateMapper.map(inputs.events, inputs.routines, now, zone, inputs.left, inputs.estimation)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private fun active(session: ActiveSession, now: Long): HomeUiState? {
        val view = session.machine.view(session.state, now) ?: return null
        return HomeUiState.Active(
            sessionId = session.sessionId,
            title = session.event.title,
            eventAt = session.event.startAt,
            step = view.step,
            minutesLeft = view.minutesLeft,
            remainingFraction = view.remainingFraction,
            isLate = view.isLate,
            minutesLate = view.minutesLate,
        )
    }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1_000L
    }
}
