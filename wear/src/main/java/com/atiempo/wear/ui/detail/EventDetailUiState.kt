package com.atiempo.wear.ui.detail

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.data.repo.EstimationData
import com.atiempo.wear.data.repo.planFor
import com.atiempo.wear.domain.model.PrepStep
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Situación de un paso respecto a la hora actual. */
enum class StepStatus { UPCOMING, CURRENT, PAST }

data class TimelineStep(val step: PrepStep, val startAt: Long, val status: StepStatus)

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    /** El evento no existe (por ejemplo, se borró). */
    data object NotFound : EventDetailUiState

    data class Loaded(
        val eventId: Long,
        val title: String,
        val eventAt: Long,
        val dayOffset: Int,
        val weekday: DayOfWeek,
        val place: String?,
        val steps: List<TimelineStep>,
        val arriveAt: Long,
        /** Se puede empezar la preparación antes de tiempo mientras el evento no haya empezado. */
        val canStart: Boolean,
    ) : EventDetailUiState
}

/** Lógica pura del Detalle del evento (DISENO_UI §3.3). */
object EventDetailStateMapper {

    fun map(
        event: Event?,
        routines: List<Routine>,
        now: Long,
        zone: ZoneId,
        data: EstimationData = EstimationData.NONE,
    ): EventDetailUiState {
        val routine = event?.let { e -> routines.firstOrNull { it.id == e.routineId } }
            ?: return EventDetailUiState.NotFound
        val plan = routine.planFor(event, data)
        val starts = PrepStep.entries.map { plan.startOf(it) }
        // Cada paso dura hasta que empieza el siguiente; el último, hasta la hora del evento.
        val ends = starts.drop(1) + plan.eventAt
        val steps = PrepStep.entries.mapIndexed { i, step ->
            val status = when {
                now >= ends[i] -> StepStatus.PAST
                now >= starts[i] -> StepStatus.CURRENT
                else -> StepStatus.UPCOMING
            }
            TimelineStep(step, starts[i], status)
        }
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val eventDate = Instant.ofEpochMilli(event.startAt).atZone(zone).toLocalDate()
        return EventDetailUiState.Loaded(
            eventId = event.id,
            title = event.title,
            eventAt = event.startAt,
            dayOffset = ChronoUnit.DAYS.between(today, eventDate).toInt(),
            weekday = eventDate.dayOfWeek,
            place = event.place,
            steps = steps,
            arriveAt = plan.arriveAt,
            canStart = now < event.startAt,
        )
    }
}
