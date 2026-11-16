package com.atiempo.wear.ui.routines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.theme.AppTheme
import com.atiempo.wear.ui.theme.CardCorner

// ---------- 3.10 Rutinas ----------

/** 3.10 Rutinas (`routines`). */
@Composable
fun RoutinesRoute(container: AppContainer, onEditRoutine: (routineId: Long) -> Unit) {
    val viewModel: RoutinesViewModel = viewModel(factory = viewModelFactory { initializer { RoutinesViewModel(container.routines, container.history) } })
    val state by viewModel.state.collectAsStateWithLifecycle()
    RoutinesScreen(state.orEmpty(), onEditRoutine)
}

/** Las 3 rutinas con sus 4 duraciones (barra de colores de los estados) y sus objetos. */
@Composable
fun RoutinesScreen(routines: List<RoutineSummary>, onEditRoutine: (routineId: Long) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec)) {
                    Text(text = stringResource(R.string.routines_title), style = AppTheme.type.title, color = AppTheme.colors.onSurface)
                }
            }
            routines.forEach { routine ->
                item(key = routine.id) {
                    Card(
                        onClick = { onEditRoutine(routine.id) },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface, contentColor = AppTheme.colors.onSurface),
                        border = BorderStroke(1.dp, AppTheme.colors.surfaceBright),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = routine.name, style = AppTheme.type.button, color = AppTheme.colors.onSurface, modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(R.string.routines_total, routine.totalMinutes),
                                style = AppTheme.type.label.copy(fontFeatureSettings = "tnum"),
                                color = AppTheme.colors.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        StepBar(routine.stepMinutes)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = routine.items.joinToString(" · "),
                            style = AppTheme.type.caption,
                            color = AppTheme.colors.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Barra con un tramo por paso, del color de su estado y de largo proporcional a sus minutos. */
@Composable
private fun StepBar(minutes: List<Int>) {
    val labels = PrepStep.entries.map { stepLabel(it) }
    val description = labels.zip(minutes).joinToString(", ") { (label, min) -> "$label $min min" }
    Row(
        modifier = Modifier.fillMaxWidth().height(8.dp).semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        PrepStep.entries.zip(minutes).forEach { (step, min) ->
            Box(
                modifier = Modifier
                    .weight(min.coerceAtLeast(1).toFloat())
                    .height(8.dp)
                    .background(AppTheme.stateColors.of(step), CircleShape),
            )
        }
    }
}

// ---------- 3.9 Ajustar estimaciones ----------

/** 3.9 Ajustar estimaciones (`routine/{routineId}/edit`). */
@Composable
fun RoutineEditRoute(container: AppContainer, routineId: Long) {
    val viewModel: RoutineEditViewModel = viewModel(
        key = "routine-$routineId",
        factory = viewModelFactory { initializer { RoutineEditViewModel(routineId, container.routines, container.history) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    RoutineEditScreen(state, onChange = viewModel::change, onReset = viewModel::reset)
}

/**
 * Por cada paso, un selector de minutos con − y + (DISENO_UI §3.9), lo aprendido y el ajuste de la persona.
 * Los cambios se guardan al instante.
 */
@Composable
fun RoutineEditScreen(
    state: RoutineEditUiState?,
    onChange: (PrepStep, Int) -> Unit,
    onReset: (PrepStep) -> Unit,
) {
    if (state == null) {
        Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background))
        return
    }
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec)) {
                    Text(text = state.name, style = AppTheme.type.title, color = AppTheme.colors.onSurface)
                }
            }
            item {
                Text(
                    text = stringResource(R.string.routine_edit_hint),
                    style = AppTheme.type.label,
                    color = AppTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                )
            }
            state.steps.forEach { estimate ->
                item(key = estimate.step.name) { StepEditor(estimate, onChange, onReset) }
            }
        }
    }
}

@Composable
private fun StepEditor(estimate: StepEstimate, onChange: (PrepStep, Int) -> Unit, onReset: (PrepStep) -> Unit) {
    val color = AppTheme.stateColors.of(estimate.step)
    val name = stepLabel(estimate.step)
    val shape = RoundedCornerShape(CardCorner)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(AppTheme.colors.surface, shape)
            .border(BorderStroke(1.dp, AppTheme.colors.surfaceBright), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(text = name, style = AppTheme.type.label, color = AppTheme.colors.onSurface)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StepperButton(R.drawable.ic_remove, stringResource(R.string.routine_less)) { onChange(estimate.step, -1) }
            val description = pluralStringResource(R.plurals.a11y_step_minutes, estimate.minutes, name, estimate.minutes)
            Row(modifier = Modifier.clearAndSetSemantics { contentDescription = description }) {
                Text(
                    text = estimate.minutes.toString(),
                    style = AppTheme.type.title.copy(fontFeatureSettings = "tnum"),
                    color = AppTheme.colors.onSurface,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = stringResource(R.string.unit_min),
                    style = AppTheme.type.label,
                    color = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline().padding(start = 3.dp),
                )
            }
            StepperButton(R.drawable.ic_add, stringResource(R.string.routine_more)) { onChange(estimate.step, +1) }
        }
        Text(
            text = estimate.learnedMin?.let { stringResource(R.string.routine_learned, it) } ?: stringResource(R.string.routine_no_history),
            style = AppTheme.type.caption,
            color = AppTheme.colors.onSurfaceVariant,
        )
        if (estimate.manualMin != null) {
            Text(text = stringResource(R.string.routine_your_value, estimate.manualMin), style = AppTheme.type.caption, color = color)
            if (estimate.learnedMin != null) {
                TextButton(
                    onClick = { onReset(estimate.step) },
                    colors = TextButtonDefaults.textButtonColors(contentColor = AppTheme.colors.primary),
                    modifier = Modifier.height(36.dp),
                ) {
                    Text(text = stringResource(R.string.routine_use_learned), style = AppTheme.type.label)
                }
            }
        }
    }
}

/** Botón redondo de − o +, de 48 dp (objetivo táctil mínimo). */
@Composable
private fun StepperButton(icon: Int, description: String, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = AppTheme.colors.surfaceBright,
            contentColor = AppTheme.colors.onSurface,
        ),
    ) {
        Icon(painterResource(icon), contentDescription = description, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun stepLabel(step: PrepStep): String = stringResource(
    when (step) {
        PrepStep.FINISH -> R.string.step_finish
        PrepStep.PREPARE -> R.string.step_prepare
        PrepStep.CHECK -> R.string.step_check
        PrepStep.LEAVE -> R.string.step_travel
    },
)

// ---------- Previews ----------

private val previewRoutines = listOf(
    RoutineSummary(1, "Clase", listOf(10, 15, 5, 12), listOf("Llaves", "Billetera", "Carné", "Cuaderno")),
    RoutineSummary(2, "Cita", listOf(5, 15, 5, 20), listOf("Llaves", "Billetera", "Documento", "Orden médica")),
    RoutineSummary(3, "Salida social", listOf(10, 30, 5, 25), listOf("Llaves", "Billetera", "Celular", "Chaqueta")),
)
private val previewEdit = RoutineEditUiState(
    name = "Clase",
    steps = listOf(
        StepEstimate(PrepStep.FINISH, 10, 9, null),
        StepEstimate(PrepStep.PREPARE, 15, 18, 15),
        StepEstimate(PrepStep.CHECK, 5, 7, 5),
        StepEstimate(PrepStep.LEAVE, 12, null, null),
    ),
)

@WearPreviews
@Composable
private fun RoutinesLightPreview() = ATiempoPreview(dark = false) { RoutinesScreen(previewRoutines) {} }

@WearPreviews
@Composable
private fun RoutinesDarkPreview() = ATiempoPreview(dark = true) { RoutinesScreen(previewRoutines) {} }

@WearPreviews
@Composable
private fun RoutineEditLightPreview() = ATiempoPreview(dark = false) { RoutineEditScreen(previewEdit, { _, _ -> }, {}) }

@WearPreviews
@Composable
private fun RoutineEditDarkPreview() = ATiempoPreview(dark = true) { RoutineEditScreen(previewEdit, { _, _ -> }, {}) }
