package com.atiempo.wear.ui.home

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

class HomeViewModel(
    events: EventRepository,
    routines: RoutineRepository,
    clock: Clock,
    zone: ZoneId,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        // Desde un día atrás: el filtro fino por "ahora" lo hace el mapper en cada tic.
        events.upcoming(from = clock.now() - DAY_MS),
        routines.all(),
        clock.ticks(),
    ) { eventList, routineList, now ->
        HomeStateMapper.map(eventList, routineList, now, zone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1_000L
    }
}
