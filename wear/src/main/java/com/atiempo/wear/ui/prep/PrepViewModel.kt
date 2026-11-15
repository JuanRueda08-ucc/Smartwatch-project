package com.atiempo.wear.ui.prep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atiempo.wear.data.session.ActiveSession
import com.atiempo.wear.data.session.PrepSessionManager
import com.atiempo.wear.domain.PrepPhase
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.domain.time.Clock
import com.atiempo.wear.domain.time.ticks
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface PrepUiState {
    data object Loading : PrepUiState

    /** La sesión ya no está activa (por ejemplo, se terminó y se cerró el resultado). */
    data object Missing : PrepUiState

    /** La persona tocó "Ya salí": la pantalla pasa al resultado. */
    data object Finished : PrepUiState

    data class Active(
        val eventTitle: String,
        val step: PrepStep,
        val minutesLeft: Int,
        val remainingFraction: Float,
        val isLate: Boolean,
        val minutesLate: Int,
        /** Llegada estimada si sale ahora (o a la hora planeada, si aún no es hora). */
        val arriveAt: Long,
        val items: List<String>,
        val checkedItems: Set<Int>,
    ) : PrepUiState
}

class PrepViewModel(
    private val sessionId: Long,
    private val manager: PrepSessionManager,
    clock: Clock,
) : ViewModel() {

    val state: StateFlow<PrepUiState> = combine(manager.active, clock.ticks()) { session, now ->
        map(session, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrepUiState.Loading)

    fun onConfirm() = manager.confirm()

    fun onToggleItem(index: Int) = manager.toggleItem(index)

    private fun map(session: ActiveSession?, now: Long): PrepUiState {
        if (session == null || session.sessionId != sessionId) return PrepUiState.Missing
        if (session.state.phase == PrepPhase.DONE) return PrepUiState.Finished
        val view = session.machine.view(session.state, now) ?: return PrepUiState.Loading
        val travel = session.plan.arriveAt - session.plan.leaveAt
        return PrepUiState.Active(
            eventTitle = session.event.title,
            step = view.step,
            minutesLeft = view.minutesLeft,
            remainingFraction = view.remainingFraction,
            isLate = view.isLate,
            minutesLate = view.minutesLate,
            // "Sal ahora · Llegas a las…": si sale ya, aunque sea antes de la hora del plan (igual que el resultado).
            arriveAt = now + travel,
            items = session.routine.items,
            checkedItems = session.checkedItems,
        )
    }
}
