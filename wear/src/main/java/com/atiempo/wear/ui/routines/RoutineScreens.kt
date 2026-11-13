package com.atiempo.wear.ui.routines

import androidx.compose.runtime.Composable
import com.atiempo.wear.R
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.PlaceholderLink
import com.atiempo.wear.ui.components.PlaceholderScreen
import com.atiempo.wear.ui.components.WearPreviews

/** 3.10 Rutinas (`routines`). Provisional hasta la tarea 3.10. */
@Composable
fun RoutinesScreen(onEditRoutine: () -> Unit) {
    PlaceholderScreen(
        title = R.string.routines_title,
        description = R.string.routines_placeholder,
        links = listOf(PlaceholderLink(R.string.action_adjust_routine, onEditRoutine)),
    )
}

/** 3.9 Ajustar estimaciones (`routine/{routineId}/edit`). Provisional hasta la tarea 3.10. */
@Composable
fun RoutineEditScreen() {
    PlaceholderScreen(title = R.string.routine_edit_title, description = R.string.routine_edit_placeholder)
}

@WearPreviews
@Composable
private fun RoutinesLightPreview() = ATiempoPreview(dark = false) { RoutinesScreen {} }

@WearPreviews
@Composable
private fun RoutinesDarkPreview() = ATiempoPreview(dark = true) { RoutinesScreen {} }

@WearPreviews
@Composable
private fun RoutineEditLightPreview() = ATiempoPreview(dark = false) { RoutineEditScreen() }

@WearPreviews
@Composable
private fun RoutineEditDarkPreview() = ATiempoPreview(dark = true) { RoutineEditScreen() }
