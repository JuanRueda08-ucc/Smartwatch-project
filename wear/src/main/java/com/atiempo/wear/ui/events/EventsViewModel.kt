package com.atiempo.wear.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atiempo.wear.data.repo.EventRepository
import com.atiempo.wear.data.repo.RoutineRepository
import com.atiempo.wear.domain.time.Clock
import com.atiempo.wear.domain.time.ticks
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

class EventsViewModel(
    events: EventRepository,
    routines: RoutineRepository,
    clock: Clock,
    zone: ZoneId,
) : ViewModel() {

    val state: StateFlow<EventsUiState> = combine(
        events.upcoming(from = clock.now() - DAY_MS),
        routines.all(),
        // La lista solo cambia cuando un evento pasa: basta revisar cada 15 s.
        clock.ticks(periodMs = 15_000),
    ) { eventList, routineList, now ->
        EventsStateMapper.map(eventList, routineList, now, zone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState.Loading)

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1_000L
    }
}
