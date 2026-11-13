package com.atiempo.wear.ui.events

import androidx.compose.runtime.Composable
import com.atiempo.wear.R
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.PlaceholderLink
import com.atiempo.wear.ui.components.PlaceholderScreen
import com.atiempo.wear.ui.components.WearPreviews

/** 3.2 Lista de eventos (`events`). Provisional hasta la tarea 3.2. */
@Composable
fun EventsScreen(
    onNewEvent: () -> Unit,
    onOpenEvent: () -> Unit,
    onRoutines: () -> Unit,
    onSummary: () -> Unit,
    onSettings: () -> Unit,
) {
    PlaceholderScreen(
        title = R.string.events_title,
        description = R.string.events_placeholder,
        links = listOf(
            PlaceholderLink(R.string.action_new_event, onNewEvent),
            PlaceholderLink(R.string.action_view_event, onOpenEvent),
            PlaceholderLink(R.string.action_routines, onRoutines),
            PlaceholderLink(R.string.action_summary, onSummary),
            PlaceholderLink(R.string.action_settings, onSettings),
        ),
    )
}

@WearPreviews
@Composable
private fun EventsLightPreview() = ATiempoPreview(dark = false) { EventsScreen({}, {}, {}, {}, {}) }

@WearPreviews
@Composable
private fun EventsDarkPreview() = ATiempoPreview(dark = true) { EventsScreen({}, {}, {}, {}, {}) }
