package com.atiempo.wear.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atiempo.wear.data.repo.EstimationData
import com.atiempo.wear.data.repo.EventRepository
import com.atiempo.wear.data.repo.RoutineRepository
import com.atiempo.wear.domain.time.Clock
import com.atiempo.wear.domain.time.ticks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

class EventDetailViewModel(
    eventId: Long,
    events: EventRepository,
    routines: RoutineRepository,
    clock: Clock,
    zone: ZoneId,
    estimation: Flow<EstimationData>,
) : ViewModel() {

    val state: StateFlow<EventDetailUiState> = combine(
        events.event(eventId),
        routines.all(),
        // El paso resaltado cambia por minutos: basta revisar cada 15 s.
        clock.ticks(periodMs = 15_000),
        estimation,
    ) { event, routineList, now, data ->
        EventDetailStateMapper.map(event, routineList, now, zone, data)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventDetailUiState.Loading)
}
