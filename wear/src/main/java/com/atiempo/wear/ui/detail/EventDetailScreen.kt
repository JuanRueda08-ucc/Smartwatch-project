package com.atiempo.wear.ui.detail

import androidx.compose.runtime.Composable
import com.atiempo.wear.R
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.PlaceholderLink
import com.atiempo.wear.ui.components.PlaceholderScreen
import com.atiempo.wear.ui.components.WearPreviews

/** 3.3 Detalle del evento (`event/{eventId}`). Provisional hasta la tarea 3.3. */
@Composable
fun EventDetailScreen(onWhyThisTime: () -> Unit, onStartNow: () -> Unit) {
    PlaceholderScreen(
        title = R.string.detail_title,
        description = R.string.detail_placeholder,
        links = listOf(
            PlaceholderLink(R.string.action_why_this_time, onWhyThisTime),
            PlaceholderLink(R.string.action_start_now, onStartNow),
        ),
    )
}

@WearPreviews
@Composable
private fun EventDetailLightPreview() = ATiempoPreview(dark = false) { EventDetailScreen({}, {}) }

@WearPreviews
@Composable
private fun EventDetailDarkPreview() = ATiempoPreview(dark = true) { EventDetailScreen({}, {}) }
