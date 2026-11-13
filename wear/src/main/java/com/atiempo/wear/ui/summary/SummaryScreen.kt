package com.atiempo.wear.ui.summary

import androidx.compose.runtime.Composable
import com.atiempo.wear.R
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.PlaceholderScreen
import com.atiempo.wear.ui.components.WearPreviews

/** 3.11 Resumen semanal (`summary`). Provisional hasta la tarea 3.11. */
@Composable
fun SummaryScreen() {
    PlaceholderScreen(title = R.string.summary_title, description = R.string.summary_placeholder)
}

@WearPreviews
@Composable
private fun SummaryLightPreview() = ATiempoPreview(dark = false) { SummaryScreen() }

@WearPreviews
@Composable
private fun SummaryDarkPreview() = ATiempoPreview(dark = true) { SummaryScreen() }
