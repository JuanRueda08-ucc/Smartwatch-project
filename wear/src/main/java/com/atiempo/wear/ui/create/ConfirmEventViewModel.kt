package com.atiempo.wear.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atiempo.wear.data.draft.DraftStore
import com.atiempo.wear.data.repo.EventRepository
import com.atiempo.wear.data.repo.RoutineRepository
import com.atiempo.wear.data.repo.isPast
import com.atiempo.wear.data.repo.toEvent
import com.atiempo.wear.domain.model.Parsed
import com.atiempo.wear.domain.model.ParsedEvent
import com.atiempo.wear.domain.model.RoutineKind
import com.atiempo.wear.domain.time.Clock
import com.atiempo.wear.domain.time.ticks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class ConfirmUiState(
    /** null si no hay nada que confirmar (por ejemplo, la app se reinició). */
    val draft: ParsedEvent?,
    /** La fecha y hora elegidas ya pasaron: no se puede guardar. */
    val isPast: Boolean = false,
    val saved: Boolean = false,
    /** Hoy, para mostrar "Hoy", "Mañana" o "Sábado 21". */
    val today: LocalDate = LocalDate.now(),
)

/** 3.5 Confirmar evento: cada cambio de la persona deja ese dato como seguro. */
class ConfirmEventViewModel(
    private val drafts: DraftStore,
    private val events: EventRepository,
    private val routines: RoutineRepository,
    clock: Clock,
    private val zone: ZoneId,
) : ViewModel() {

    private val saved = MutableStateFlow(false)

    val state: StateFlow<ConfirmUiState> = combine(drafts.draft, clock.ticks(periodMs = 15_000), saved) { draft, now, isSaved ->
        ConfirmUiState(
            draft = draft,
            isPast = draft?.isPast(now, zone) == true,
            saved = isSaved,
            today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConfirmUiState(drafts.draft.value))

    fun setTitle(title: String) {
        val clean = title.trim()
        if (clean.isNotEmpty()) drafts.update { it.copy(title = Parsed(clean, true)) }
    }

    fun setDate(date: LocalDate) = drafts.update { it.copy(date = Parsed(date, true)) }

    fun setTime(time: LocalTime) = drafts.update { it.copy(time = Parsed(time, true)) }

    fun setRoutine(kind: RoutineKind) = drafts.update { it.copy(routine = Parsed(kind, true)) }

    /** Lugar dictado, o null si la persona confirma que no hay lugar. */
    fun setPlace(place: String?) = drafts.update { it.copy(place = Parsed(place?.trim()?.ifEmpty { null }, true)) }

    fun save() {
        val current = state.value
        val draft = current.draft ?: return
        if (current.isPast || current.saved) return
        viewModelScope.launch {
            val event = draft.toEvent(routines.all().first(), zone) ?: return@launch
            events.add(event)
            saved.value = true
        }
    }

    /** Al salir de la pantalla tras guardar, el borrador ya no sirve. */
    fun onLeft() = drafts.clear()
}
