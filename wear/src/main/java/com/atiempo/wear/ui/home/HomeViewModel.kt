package com.atiempo.wear.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atiempo.wear.data.repo.EventRepository
import com.atiempo.wear.data.repo.HistoryRepository
import com.atiempo.wear.data.repo.RoutineRepository
import com.atiempo.wear.data.session.ActiveSession
import com.atiempo.wear.data.session.PrepSessionManager
import com.atiempo.wear.domain.PrepPhase
import com.atiempo.wear.domain.time.Clock
import com.atiempo.wear.domain.time.ticks
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
) : ViewModel() {

    // Lo que cambia sin el reloj: eventos, rutinas y para cuáles ya se salió.
    private val data = combine(events.upcoming(from = clock.now() - DAY_MS), routines.all(), history.leftEventIds()) { e, r, left ->
        Triple(e, r, left)
    }

    val state: StateFlow<HomeUiState> = combine(data, clock.ticks(), prep.active) { (eventList, routineList, left), now, session ->
        session?.takeIf { it.state.phase == PrepPhase.ACTIVE }?.let { active(it, now) }
            ?: HomeStateMapper.map(eventList, routineList, now, zone, left)
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
