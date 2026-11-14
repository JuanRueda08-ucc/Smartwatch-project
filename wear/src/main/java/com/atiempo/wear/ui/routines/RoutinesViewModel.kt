package com.atiempo.wear.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.data.repo.HistoryRepository
import com.atiempo.wear.data.repo.RoutineRepository
import com.atiempo.wear.data.repo.baseMinutes
import com.atiempo.wear.data.repo.defaultFor
import com.atiempo.wear.data.repo.manualFor
import com.atiempo.wear.data.repo.withManual
import com.atiempo.wear.domain.LearnedDuration
import com.atiempo.wear.domain.model.PrepStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Una rutina en la lista: nombre, minutos de cada paso y objetos. */
data class RoutineSummary(val id: Long, val name: String, val stepMinutes: List<Int>, val items: List<String>) {
    val totalMinutes: Int get() = stepMinutes.sum()
}

/**
 * 3.10 Rutinas. Cada paso usa la misma regla que el editor: el ajuste manual, si no lo aprendido del
 * historial, si no el valor por defecto; así la lista y el editor muestran los mismos minutos.
 */
class RoutinesViewModel(routines: RoutineRepository, private val history: HistoryRepository) : ViewModel() {

    val state: StateFlow<List<RoutineSummary>?> = routines.all().map { list ->
        list.map { r ->
            val minutes = PrepStep.entries.map { step ->
                r.manualFor(step) ?: LearnedDuration.minutesOr(r.defaultFor(step), history.history(r.id, step).map { it.actualMin })
            }
            RoutineSummary(r.id, r.name, minutes, r.items)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Estimación de un paso en el editor. */
data class StepEstimate(
    val step: PrepStep,
    /** Lo que se usa: el ajuste manual, si no lo aprendido, si no el valor por defecto. */
    val minutes: Int,
    /** Base aprendida del historial (EMA); null sin historial. */
    val learnedMin: Int?,
    val manualMin: Int?,
)

data class RoutineEditUiState(val name: String, val steps: List<StepEstimate>)

/** 3.9 Ajustar estimaciones: cada toque en − o + se guarda al instante. */
class RoutineEditViewModel(
    private val routineId: Long,
    private val routines: RoutineRepository,
    history: HistoryRepository,
) : ViewModel() {

    private val learned = MutableStateFlow<Map<PrepStep, Int?>>(emptyMap())

    init {
        viewModelScope.launch {
            learned.value = PrepStep.entries.associateWith { step ->
                LearnedDuration.ema(history.history(routineId, step).map { it.actualMin })?.let { kotlin.math.round(it).toInt() }
            }
        }
    }

    val state: StateFlow<RoutineEditUiState?> = combine(routines.routine(routineId), learned) { routine, learnedMap ->
        routine?.let { r ->
            RoutineEditUiState(
                name = r.name,
                steps = PrepStep.entries.map { step ->
                    val learnedMin = learnedMap[step]
                    StepEstimate(step, r.manualFor(step) ?: learnedMin ?: r.defaultFor(step), learnedMin, r.manualFor(step))
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** − o +: deja un ajuste manual a partir del valor que se ve, entre 1 y 90 min. */
    fun change(step: PrepStep, delta: Int) = update { r ->
        val current = state.value?.steps?.firstOrNull { it.step == step }?.minutes ?: r.baseMinutes(step)
        r.withManual(step, (current + delta).coerceIn(MIN_MINUTES, MAX_MINUTES))
    }

    /** "Usar lo aprendido": quita el ajuste manual del paso. */
    fun reset(step: PrepStep) = update { it.withManual(step, null) }

    private fun update(transform: (Routine) -> Routine) {
        viewModelScope.launch {
            val routine = routines.routine(routineId).first() ?: return@launch
            routines.update(transform(routine))
        }
    }

    private companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 90
    }
}
