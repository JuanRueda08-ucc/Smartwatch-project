package com.atiempo.wear.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.atiempo.wear.R
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.ui.theme.AppTheme

/* Cómo se ve cada estado (DISENO_UI §2 y §3.6): color, ícono y nombre. */

@DrawableRes
fun PrepStep.icon(): Int = when (this) {
    PrepStep.FINISH -> R.drawable.ic_hourglass_bottom
    PrepStep.PREPARE -> R.drawable.ic_checkroom
    PrepStep.CHECK -> R.drawable.ic_backpack
    PrepStep.LEAVE -> R.drawable.ic_directions_walk
}

/** Nombre en la pantalla del estado ("Sal ahora"). */
@StringRes
fun PrepStep.title(): Int = when (this) {
    PrepStep.FINISH -> R.string.step_finish
    PrepStep.PREPARE -> R.string.step_prepare
    PrepStep.CHECK -> R.string.step_check
    PrepStep.LEAVE -> R.string.step_leave
}

/** Color del estado, o `late` si ya va tarde. */
@Composable
fun stepColor(step: PrepStep, late: Boolean = false): Color =
    if (late) AppTheme.stateColors.late else AppTheme.stateColors.of(step)
