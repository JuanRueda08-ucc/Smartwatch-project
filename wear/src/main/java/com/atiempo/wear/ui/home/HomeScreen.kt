package com.atiempo.wear.ui.home

import androidx.compose.runtime.Composable
import com.atiempo.wear.R
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.PlaceholderLink
import com.atiempo.wear.ui.components.PlaceholderScreen
import com.atiempo.wear.ui.components.WearPreviews

/** 3.1 Inicio (`home`). Provisional hasta la tarea 3.1. */
@Composable
fun HomeScreen(onViewPlan: () -> Unit, onViewEvents: () -> Unit) {
    PlaceholderScreen(
        title = R.string.home_title,
        description = R.string.home_placeholder,
        links = listOf(
            PlaceholderLink(R.string.action_view_plan, onViewPlan),
            PlaceholderLink(R.string.action_view_events, onViewEvents),
        ),
    )
}

@WearPreviews
@Composable
private fun HomeLightPreview() = ATiempoPreview(dark = false) { HomeScreen({}, {}) }

@WearPreviews
@Composable
private fun HomeDarkPreview() = ATiempoPreview(dark = true) { HomeScreen({}, {}) }
