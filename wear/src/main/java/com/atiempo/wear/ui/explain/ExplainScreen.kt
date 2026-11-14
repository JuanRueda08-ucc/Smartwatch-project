package com.atiempo.wear.ui.explain

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.domain.model.Adjustment
import com.atiempo.wear.domain.model.AdjustmentReason
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.domain.model.TrafficLevel
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.TintedIcon
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.theme.AppTheme
import com.atiempo.wear.ui.theme.CardCorner
import java.time.DayOfWeek
import kotlin.math.abs

/** 3.8 ¿Por qué te avisé antes? (`explain/{eventId}`): conecta la pantalla con su ViewModel. */
@Composable
fun ExplainRoute(container: AppContainer, eventId: Long, onNotRight: (routineId: Long) -> Unit) {
    val viewModel: ExplainViewModel = viewModel(
        key = "explain-$eventId",
        factory = viewModelFactory { initializer { ExplainViewModel(eventId, container.events, container.routines, container.estimation) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExplainScreen(state, onNotRight)
}

/**
 * Lista de razones con su efecto en minutos, de mayor a menor (DISENO_UI §3.8, principio "Transparente"),
 * el total y "No es así" para corregir la rutina.
 */
@Composable
fun ExplainScreen(state: ExplainUiState, onNotRight: (routineId: Long) -> Unit) {
    val loaded = state as? ExplainUiState.Loaded
    if (loaded == null) {
        Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background))
        return
    }
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec)) {
                    Text(
                        text = stringResource(R.string.explain_title),
                        style = AppTheme.type.title,
                        color = AppTheme.colors.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            loaded.adjustments.forEach { adjustment ->
                item { ReasonCard(adjustment) }
            }
            item {
                val total = loaded.totalMinutes
                Text(
                    text = if (total >= 0) stringResource(R.string.explain_total_before, total)
                    else stringResource(R.string.explain_total_after, abs(total)),
                    style = AppTheme.type.label,
                    color = AppTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
            }
            item {
                Button(
                    onClick = { onNotRight(loaded.routineId) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.primary,
                        contentColor = AppTheme.colors.onState,
                        iconColor = AppTheme.colors.onState,
                    ),
                    icon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = { Text(text = stringResource(R.string.action_not_right), style = AppTheme.type.button) },
                )
            }
        }
    }
}

/** Una razón: ícono, efecto en minutos y la frase, en una tarjeta. No es tocable. */
@Composable
private fun ReasonCard(adjustment: Adjustment) {
    val text = reasonText(adjustment.reason)
    val minutes = if (adjustment.minutes >= 0) stringResource(R.string.explain_minutes_plus, adjustment.minutes)
    else stringResource(R.string.explain_minutes_minus, abs(adjustment.minutes))
    val shape = RoundedCornerShape(CardCorner)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(AppTheme.colors.surface, shape)
            .border(BorderStroke(1.dp, AppTheme.colors.surfaceBright), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = "$minutes. $text" },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TintedIcon(icon = adjustment.reason.icon(), color = AppTheme.colors.primary, size = 28)
            Spacer(Modifier.size(8.dp))
            Text(text = minutes, style = AppTheme.type.button.copy(fontFeatureSettings = "tnum"), color = AppTheme.colors.primary)
        }
        Spacer(Modifier.height(4.dp))
        Text(text = text, style = AppTheme.type.body, color = AppTheme.colors.onSurface)
    }
}

@DrawableRes
private fun AdjustmentReason.icon(): Int = when (this) {
    is AdjustmentReason.Learned -> R.drawable.ic_history
    is AdjustmentReason.DayEffect -> R.drawable.ic_calendar_month
    AdjustmentReason.Rain -> R.drawable.ic_rainy
    is AdjustmentReason.Traffic -> R.drawable.ic_traffic
    is AdjustmentReason.Manual -> R.drawable.ic_edit
    AdjustmentReason.SafetyMargin -> R.drawable.ic_timer
}

/** La frase de cada razón, en términos de la persona (DISENO_UI §5). */
@Composable
private fun reasonText(reason: AdjustmentReason): String = when (reason) {
    is AdjustmentReason.Learned -> stringResource(R.string.explain_learned, reason.learnedMin, stepVerb(reason.step))
    is AdjustmentReason.DayEffect ->
        stringResource(R.string.explain_day, stringArrayResource(R.array.weekdays_plural)[reason.day.value - 1], stepVerb(reason.step))
    AdjustmentReason.Rain -> stringResource(R.string.explain_rain)
    is AdjustmentReason.Traffic -> {
        val place = reason.place ?: stringResource(R.string.explain_traffic_somewhere)
        if (reason.level == TrafficLevel.HIGH) stringResource(R.string.explain_traffic_high, place)
        else stringResource(R.string.explain_traffic_medium, place)
    }
    is AdjustmentReason.Manual -> stringResource(R.string.explain_manual, stepName(reason.step), reason.manualMin)
    AdjustmentReason.SafetyMargin -> stringResource(R.string.explain_margin)
}

@Composable
private fun stepVerb(step: PrepStep): String = stringResource(
    when (step) {
        PrepStep.FINISH -> R.string.step_verb_finish
        PrepStep.PREPARE -> R.string.step_verb_prepare
        PrepStep.CHECK -> R.string.step_verb_check
        PrepStep.LEAVE -> R.string.step_verb_leave
    },
)

/** Nombre del paso tal como se ajusta: el último es el trayecto. */
@Composable
private fun stepName(step: PrepStep): String = stringResource(
    when (step) {
        PrepStep.FINISH -> R.string.step_finish
        PrepStep.PREPARE -> R.string.step_prepare
        PrepStep.CHECK -> R.string.step_check
        PrepStep.LEAVE -> R.string.step_travel
    },
)

// ---------- Previews ----------

/** Las razones del ejemplo de DISENO_UI §3.8, como las dará el estimador en la Fase 4. */
private val previewState = ExplainUiState.Loaded(
    routineId = 1,
    adjustments = listOf(
        Adjustment(AdjustmentReason.DayEffect(DayOfWeek.MONDAY, PrepStep.PREPARE), 6),
        Adjustment(AdjustmentReason.Traffic(TrafficLevel.HIGH, "la universidad"), 5),
        Adjustment(AdjustmentReason.Rain, 4),
        Adjustment(AdjustmentReason.SafetyMargin, 5),
        Adjustment(AdjustmentReason.Manual(PrepStep.CHECK, 5), -2),
    ),
)

@WearPreviews
@Composable
private fun ExplainLightPreview() = ATiempoPreview(dark = false) { ExplainScreen(previewState) {} }

@WearPreviews
@Composable
private fun ExplainDarkPreview() = ATiempoPreview(dark = true) { ExplainScreen(previewState) {} }
