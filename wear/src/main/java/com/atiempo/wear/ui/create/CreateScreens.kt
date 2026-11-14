package com.atiempo.wear.ui.create

import androidx.compose.runtime.Composable
import com.atiempo.wear.R
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.PlaceholderLink
import com.atiempo.wear.ui.components.PlaceholderScreen
import com.atiempo.wear.ui.components.WearPreviews

/** 3.5 Confirmar evento (`create/confirm`). Provisional hasta la tarea 3.5. */
@Composable
fun ConfirmEventScreen(onSave: () -> Unit) {
    PlaceholderScreen(
        title = R.string.confirm_title,
        description = R.string.confirm_placeholder,
        links = listOf(PlaceholderLink(R.string.action_save, onSave)),
    )
}

@WearPreviews
@Composable
private fun ConfirmEventLightPreview() = ATiempoPreview(dark = false) { ConfirmEventScreen {} }

@WearPreviews
@Composable
private fun ConfirmEventDarkPreview() = ATiempoPreview(dark = true) { ConfirmEventScreen {} }
