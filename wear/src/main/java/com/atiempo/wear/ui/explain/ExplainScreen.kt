package com.atiempo.wear.ui.explain

import androidx.compose.runtime.Composable
import com.atiempo.wear.R
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.PlaceholderLink
import com.atiempo.wear.ui.components.PlaceholderScreen
import com.atiempo.wear.ui.components.WearPreviews

/** 3.8 ¿Por qué te avisé antes? (`explain/{eventId}`). Provisional hasta la tarea 3.9. */
@Composable
fun ExplainScreen(onNotRight: () -> Unit) {
    PlaceholderScreen(
        title = R.string.explain_title,
        description = R.string.explain_placeholder,
        links = listOf(PlaceholderLink(R.string.action_not_right, onNotRight)),
    )
}

@WearPreviews
@Composable
private fun ExplainLightPreview() = ATiempoPreview(dark = false) { ExplainScreen {} }

@WearPreviews
@Composable
private fun ExplainDarkPreview() = ATiempoPreview(dark = true) { ExplainScreen {} }
