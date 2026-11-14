package com.atiempo.wear.ui.home

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.data.repo.planFor
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** Qué muestra Inicio (DISENO_UI §3.1). */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** No hay eventos próximos: ilustración y botón "Crear con voz". */
    data object Empty : HomeUiState

    data class Upcoming(
        val eventId: Long,
        val title: String,
        /** Hora del evento, epoch ms. */
        val eventAt: Long,
        /** 0 = hoy, 1 = mañana, 2 o más = otro día de la semana (`weekday`). */
        val dayOffset: Int,
        val weekday: DayOfWeek,
        /** Minutos que faltan para empezar a prepararse, redondeados hacia arriba; 0 si ya es hora. */
        val minutesUntilPrep: Int,
        /** Llenado del anillo exterior: 0 a 60 min antes de empezar va de 0 a 1. */
        val ringProgress: Float,
    ) : HomeUiState {
        val isDue: Boolean get() = minutesUntilPrep == 0
    }
}

/** Lógica pura de Inicio: elige el próximo evento y calcula la cuenta regresiva. */
object HomeStateMapper {
    /** El anillo empieza a llenarse una hora antes de prepararse. */
    const val RING_WINDOW_MIN = 60
    private const val MINUTE = 60_000.0

    fun map(events: List<Event>, routines: List<Routine>, now: Long, zone: ZoneId): HomeUiState {
        val routineById = routines.associateBy { it.id }
        val next = events
            .filter { it.startAt > now && it.routineId in routineById }
            .minByOrNull { it.startAt }
            ?: return HomeUiState.Empty
        val plan = routineById.getValue(next.routineId).planFor(next)
        val remainingMin = (plan.finishAt - now) / MINUTE
        val minutes = maxOf(0, ceil(remainingMin - 1e-9).toInt())
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val eventDate = Instant.ofEpochMilli(next.startAt).atZone(zone).toLocalDate()
        return HomeUiState.Upcoming(
            eventId = next.id,
            title = next.title,
            eventAt = next.startAt,
            dayOffset = ChronoUnit.DAYS.between(today, eventDate).toInt(),
            weekday = eventDate.dayOfWeek,
            minutesUntilPrep = minutes,
            ringProgress = (1 - remainingMin / RING_WINDOW_MIN).toFloat().coerceIn(0f, 1f),
        )
    }
}
