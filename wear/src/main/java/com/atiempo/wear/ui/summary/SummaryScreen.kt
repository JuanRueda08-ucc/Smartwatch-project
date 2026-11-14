package com.atiempo.wear.ui.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.data.repo.HistoryRepository
import com.atiempo.wear.domain.time.Clock
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SummaryViewModel(history: HistoryRepository, clock: Clock, zone: ZoneId) : ViewModel() {
    private val today: LocalDate = Instant.ofEpochMilli(clock.now()).atZone(zone).toLocalDate()

    val state: StateFlow<SummaryUiState?> = history.sessionsBetween(
        from = today.minusDays(SummaryMapper.DAYS.toLong()).atStartOfDay(zone).toInstant().toEpochMilli(),
        to = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
    ).map { SummaryMapper.map(it, today, zone) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** 3.11 Resumen semanal (`summary`). */
@Composable
fun SummaryRoute(container: AppContainer) {
    val viewModel: SummaryViewModel = viewModel(
        factory = viewModelFactory { initializer { SummaryViewModel(container.history, container.clock, container.zone) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    SummaryScreen(state)
}

/**
 * Porcentaje de salidas a tiempo en grande con su anillo, 7 barras coloreadas según puntualidad y una
 * frase de tendencia (DISENO_UI §3.11). Las barras "tarde" van rayadas: el estado nunca va solo por color.
 */
@Composable
fun SummaryScreen(state: SummaryUiState?) {
    Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        if (state == null) return@Box
        CircularProgressIndicator(
            progress = { state.percent / 100f },
            modifier = Modifier.fillMaxSize().padding(2.dp),
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = AppTheme.stateColors.leave,
                trackColor = AppTheme.colors.surfaceBright,
            ),
            strokeWidth = 5.dp,
        )
        val listState = rememberTransformingLazyColumnState()
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
                item { Headline(state) }
                item { WeekBars(state.days) }
                item { Legend() }
                item { Trend(state) }
                // Espacio final: la frase puede subir al centro y no queda cortada por el borde redondo.
                item { Spacer(Modifier.height(36.dp)) }
            }
        }
    }
}

@Composable
private fun Headline(state: SummaryUiState) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(R.string.summary_period), style = AppTheme.type.label, color = AppTheme.colors.onSurfaceVariant)
        Row {
            Text(
                text = state.percent.toString(),
                style = AppTheme.type.display,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = stringResource(R.string.summary_percent_unit),
                style = AppTheme.type.displayUnit,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.alignByBaseline().padding(start = 2.dp),
            )
        }
        Text(text = stringResource(R.string.summary_on_time_label), style = AppTheme.type.label, color = AppTheme.colors.onSurfaceVariant)
    }
}

/** 7 barras del más antiguo a hoy; el alto es el margen (o el retraso) del día. */
@Composable
private fun WeekBars(days: List<DaySummary>) {
    val ok = AppTheme.stateColors.leave
    val late = AppTheme.stateColors.late
    val empty = AppTheme.colors.surfaceBright
    val background = AppTheme.colors.background
    val initials = stringArrayResource(R.array.weekday_initials)
    val names = stringArrayResource(R.array.weekdays)
    val description = days.map { day ->
        val name = names[day.date.dayOfWeek.value - 1]
        when (day.result) {
            DayResult.NONE -> stringResource(R.string.a11y_summary_day_none, name)
            DayResult.ON_TIME -> stringResource(R.string.a11y_summary_day_on_time, name)
            DayResult.LATE -> stringResource(R.string.a11y_summary_day_late, name)
        }
    }.joinToString(", ")
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(modifier = Modifier.width(BARS_WIDTH).height(BARS_HEIGHT)) {
            val slot = size.width / days.size
            val barWidth = slot * 0.55f
            days.forEachIndexed { i, day ->
                val x = i * slot + (slot - barWidth) / 2
                val fraction = when (day.result) {
                    DayResult.NONE -> 0.08f
                    else -> (0.25f + day.minutes / 10f * 0.75f).coerceAtMost(1f)
                }
                val h = size.height * fraction
                val top = size.height - h
                when (day.result) {
                    DayResult.NONE -> bar(empty, x, top, barWidth, h)
                    DayResult.ON_TIME -> bar(ok, x, top, barWidth, h)
                    DayResult.LATE -> stripedBar(late, background, x, top, barWidth, h)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(modifier = Modifier.width(BARS_WIDTH)) {
            days.forEach { day ->
                Text(
                    text = initials[day.date.dayOfWeek.value - 1],
                    style = AppTheme.type.caption,
                    color = AppTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun DrawScope.bar(color: Color, x: Float, top: Float, width: Float, height: Float) {
    drawRoundRect(color, Offset(x, top), Size(width, height), CornerRadius(width / 2))
}

/** Barra "tarde": color `late` con rayas diagonales del color de fondo. */
private fun DrawScope.stripedBar(color: Color, background: Color, x: Float, top: Float, width: Float, height: Float) {
    bar(color, x, top, width, height)
    clipRect(left = x, top = top, right = x + width, bottom = top + height) {
        val step = 5.dp.toPx()
        var offset = -height
        while (offset < width) {
            drawLine(background.copy(alpha = 0.55f), Offset(x + offset, top + height), Offset(x + offset + height, top), strokeWidth = 1.5.dp.toPx())
            offset += step
        }
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AppTheme.stateColors.leave))
        Spacer(Modifier.width(4.dp))
        Text(text = stringResource(R.string.summary_legend_on_time), style = AppTheme.type.caption, color = AppTheme.colors.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        val late = AppTheme.stateColors.late
        val background = AppTheme.colors.background
        Canvas(modifier = Modifier.size(10.dp).clip(CircleShape)) { stripedBar(late, background, 0f, 0f, size.width, size.height) }
        Spacer(Modifier.width(4.dp))
        Text(text = stringResource(R.string.summary_legend_late), style = AppTheme.type.caption, color = AppTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun Trend(state: SummaryUiState) {
    val text = if (state.total == 0) {
        stringResource(R.string.summary_none)
    } else {
        val trend = pluralStringResource(R.plurals.summary_trend, state.total, state.onTime, state.total)
        val best = state.bestDay?.let { stringResource(R.string.summary_best_day, stringArrayResource(R.array.weekdays_lower)[it.value - 1]) }
        listOfNotNull(trend, best).joinToString(" ")
    }
    Text(
        text = text,
        style = AppTheme.type.body,
        color = AppTheme.colors.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp, start = 10.dp, end = 10.dp),
    )
}

private val BARS_WIDTH = 150.dp
private val BARS_HEIGHT = 56.dp

// ---------- Previews ----------

private val previewToday = LocalDate.of(2026, 11, 17)
private val previewState = SummaryUiState(
    days = listOf(
        DaySummary(previewToday.minusDays(6), DayResult.ON_TIME, 5),
        DaySummary(previewToday.minusDays(5), DayResult.ON_TIME, 8),
        DaySummary(previewToday.minusDays(4), DayResult.LATE, 4),
        DaySummary(previewToday.minusDays(3), DayResult.ON_TIME, 2),
        DaySummary(previewToday.minusDays(2), DayResult.ON_TIME, 6),
        DaySummary(previewToday.minusDays(1), DayResult.NONE, 0),
        DaySummary(previewToday, DayResult.ON_TIME, 3),
    ),
    onTime = 5,
    total = 6,
    bestDay = previewToday.minusDays(5).dayOfWeek,
)

@WearPreviews
@Composable
private fun SummaryLightPreview() = ATiempoPreview(dark = false) { SummaryScreen(previewState) }

@WearPreviews
@Composable
private fun SummaryDarkPreview() = ATiempoPreview(dark = true) { SummaryScreen(previewState) }

@WearPreviews
@Composable
private fun SummaryEmptyPreview() = ATiempoPreview(dark = false) {
    SummaryScreen(SummaryUiState(List(7) { DaySummary(previewToday.minusDays(6L - it), DayResult.NONE, 0) }, 0, 0, null))
}
