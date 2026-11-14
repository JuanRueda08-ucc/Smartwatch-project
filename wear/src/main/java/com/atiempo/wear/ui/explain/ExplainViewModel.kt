package com.atiempo.wear.ui.explain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atiempo.wear.data.repo.EventRepository
import com.atiempo.wear.data.repo.RoutineRepository
import com.atiempo.wear.data.repo.explain
import com.atiempo.wear.domain.model.Adjustment
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface ExplainUiState {
    data object Loading : ExplainUiState
    data object NotFound : ExplainUiState

    data class Loaded(
        val routineId: Long,
        /** Razones de mayor a menor efecto. */
        val adjustments: List<Adjustment>,
    ) : ExplainUiState {
        val totalMinutes: Int get() = adjustments.sumOf { it.minutes }
    }
}

class ExplainViewModel(eventId: Long, events: EventRepository, routines: RoutineRepository) : ViewModel() {

    val state: StateFlow<ExplainUiState> = combine(events.event(eventId), routines.all()) { event, routineList ->
        val routine = event?.let { e -> routineList.firstOrNull { it.id == e.routineId } }
        if (event == null || routine == null) ExplainUiState.NotFound
        else ExplainUiState.Loaded(routine.id, routine.explain(event))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExplainUiState.Loading)
}
