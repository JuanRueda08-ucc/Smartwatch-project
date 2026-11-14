package com.atiempo.wear.ui.events

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.data.repo.EstimationData
import com.atiempo.wear.data.repo.planFor
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Día de un grupo de eventos: 0 = hoy, 1 = mañana, otro = día de la semana y número. */
data class DayLabel(val dayOffset: Int, val weekday: DayOfWeek, val dayOfMonth: Int)

data class EventItem(
    val id: Long,
    val title: String,
    /** Hora del evento, epoch ms. */
    val startAt: Long,
    /** Hora de salida según el plan, epoch ms. */
    val leaveAt: Long,
    val place: String?,
)

data class EventGroup(val day: DayLabel, val events: List<EventItem>)

sealed interface EventsUiState {
    data object Loading : EventsUiState
    /** Eventos próximos agrupados por día; puede estar vacía. */
    data class Loaded(val groups: List<EventGroup>) : EventsUiState
}

/** Lógica pura de la Lista de eventos (DISENO_UI §3.2). */
object EventsStateMapper {

    fun map(
        events: List<Event>,
        routines: List<Routine>,
        now: Long,
        zone: ZoneId,
        data: EstimationData = EstimationData.NONE,
    ): EventsUiState.Loaded {
        val routineById = routines.associateBy { it.id }
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val groups = events
            .filter { it.startAt > now && it.routineId in routineById }
            .sortedBy { it.startAt }
            .groupBy { Instant.ofEpochMilli(it.startAt).atZone(zone).toLocalDate() }
            .map { (date, dayEvents) ->
                EventGroup(
                    day = DayLabel(ChronoUnit.DAYS.between(today, date).toInt(), date.dayOfWeek, date.dayOfMonth),
                    events = dayEvents.map { event ->
                        EventItem(
                            id = event.id,
                            title = event.title,
                            startAt = event.startAt,
                            leaveAt = routineById.getValue(event.routineId).planFor(event, data).leaveAt,
                            place = event.place,
                        )
                    },
                )
            }
        return EventsUiState.Loaded(groups)
    }
}
