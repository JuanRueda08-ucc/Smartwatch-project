package com.atiempo.wear.ui.prep

import androidx.compose.runtime.Composable
import com.atiempo.wear.R
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.PlaceholderLink
import com.atiempo.wear.ui.components.PlaceholderScreen
import com.atiempo.wear.ui.components.WearPreviews

/** 3.6 Estados de preparación (`prep/{sessionId}`). Provisional hasta la tarea 3.7. */
@Composable
fun PrepScreen(onLeft: () -> Unit) {
    PlaceholderScreen(
        title = R.string.prep_title,
        description = R.string.prep_placeholder,
        links = listOf(PlaceholderLink(R.string.action_left, onLeft)),
    )
}

/** 3.7 Resultado (`prep/{sessionId}/done`). Provisional hasta la tarea 3.7. */
@Composable
fun PrepDoneScreen(onDone: () -> Unit) {
    PlaceholderScreen(
        title = R.string.done_title,
        description = R.string.done_placeholder,
        links = listOf(PlaceholderLink(R.string.action_done, onDone)),
    )
}

@WearPreviews
@Composable
private fun PrepLightPreview() = ATiempoPreview(dark = false) { PrepScreen {} }

@WearPreviews
@Composable
private fun PrepDarkPreview() = ATiempoPreview(dark = true) { PrepScreen {} }

@WearPreviews
@Composable
private fun PrepDoneLightPreview() = ATiempoPreview(dark = false) { PrepDoneScreen {} }

@WearPreviews
@Composable
private fun PrepDoneDarkPreview() = ATiempoPreview(dark = true) { PrepDoneScreen {} }
