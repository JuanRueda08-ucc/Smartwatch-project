package com.atiempo.wear.ui.summary

import com.atiempo.wear.data.db.PrepSession
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/** Cómo le fue a la persona un día. */
enum class DayResult { NONE, ON_TIME, LATE }

data class DaySummary(
    val date: LocalDate,
    val result: DayResult,
    /** Minutos de margen (a tiempo) o de retraso (tarde), en promedio; 0 sin salidas. */
    val minutes: Int,
)

data class SummaryUiState(
    /** Los 7 días, del más antiguo a hoy. */
    val days: List<DaySummary>,
    val onTime: Int,
    val total: Int,
    /** Día con más margen entre los que salió a tiempo; null si ninguno. */
    val bestDay: DayOfWeek?,
) {
    /** Porcentaje de salidas a tiempo, 0 a 100. */
    val percent: Int get() = if (total == 0) 0 else (onTime * 100.0 / total).roundToInt()
}

/**
 * Lógica pura del Resumen semanal (DISENO_UI §3.11): los últimos 7 días incluido hoy (D-027).
 * Solo cuentan las salidas confirmadas ("Ya salí"); 0 minutos tarde o menos es a tiempo.
 */
object SummaryMapper {
    const val DAYS = 7

    fun map(sessions: List<PrepSession>, today: LocalDate, zone: ZoneId): SummaryUiState {
        val from = today.minusDays((DAYS - 1).toLong())
        val finished = sessions
            .filter { it.leftAt != null && it.minutesLate != null }
            .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .filterKeys { it in from..today }
        val days = (0 until DAYS).map { offset ->
            val date = from.plusDays(offset.toLong())
            val late = finished[date].orEmpty().map { it.minutesLate!! }
            when {
                late.isEmpty() -> DaySummary(date, DayResult.NONE, 0)
                late.any { it > 0 } -> DaySummary(date, DayResult.LATE, late.filter { it > 0 }.average().roundToInt())
                else -> DaySummary(date, DayResult.ON_TIME, abs(late.average()).roundToInt())
            }
        }
        val all = finished.values.flatten()
        val best = days.filter { it.result == DayResult.ON_TIME }.maxByOrNull { it.minutes }
        return SummaryUiState(
            days = days,
            onTime = all.count { it.minutesLate!! <= 0 },
            total = all.size,
            bestDay = best?.date?.dayOfWeek,
        )
    }
}
