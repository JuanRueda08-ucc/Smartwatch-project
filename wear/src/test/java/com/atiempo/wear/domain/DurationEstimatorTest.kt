package com.atiempo.wear.domain

import com.atiempo.wear.domain.model.Adjustment
import com.atiempo.wear.domain.model.AdjustmentReason
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.domain.model.TrafficLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/** Las pruebas mínimas de MODELO_ESTIMACION.md, más algunas de bordes. */
class DurationEstimatorTest {
    private val monday = DayOfWeek.MONDAY
    private fun inputs(
        prepare: StepInputs = StepInputs(15, null, emptyList()),
        travel: StepInputs = StepInputs(12, null, emptyList()),
    ) = mapOf(
        PrepStep.FINISH to StepInputs(10, null, emptyList()),
        PrepStep.PREPARE to prepare,
        PrepStep.CHECK to StepInputs(7, null, emptyList()),
        PrepStep.LEAVE to travel,
    )
    // Por defecto los registros son de domingo: así lunes y martes no tienen registros propios salvo que se pidan.
    private fun history(vararg minutes: Int, day: DayOfWeek = DayOfWeek.SUNDAY) = minutes.map { StepHistory(it, day.value) }

    @Test
    fun `sin historial devuelve los valores por defecto`() {
        val estimate = DurationEstimator.estimate(inputs(), monday)
        assertEquals(StepDurations(10, 15, 7, 12), estimate.durations)
        assertTrue(estimate.adjustments.isEmpty())
    }

    @Test
    fun `con historial constante de 20 la base converge a 20`() {
        val estimate = DurationEstimator.estimate(inputs(prepare = StepInputs(15, null, history(20, 20, 20, 20, 20))), monday)
        assertEquals(20, estimate.durations.prepare)
        assertEquals(listOf(Adjustment(AdjustmentReason.Learned(PrepStep.PREPARE, 20), 5)), estimate.adjustments)
    }

    @Test
    fun `el efecto del dia no se aplica con menos de 3 registros de ese dia`() {
        val records = history(15, 15, 15) + history(25, 25, day = monday)
        val estimate = DurationEstimator.estimate(inputs(prepare = StepInputs(15, null, records)), monday)
        assertTrue(estimate.adjustments.none { it.reason is AdjustmentReason.DayEffect })
    }

    @Test
    fun `con 3 registros del dia suma la diferencia`() {
        val records = history(15, 15, 15, 15) + history(21, 21, 21, day = monday)
        val estimate = DurationEstimator.estimate(inputs(prepare = StepInputs(15, null, records)), monday)
        val day = estimate.adjustments.single { it.reason is AdjustmentReason.DayEffect }
        assertEquals(AdjustmentReason.DayEffect(monday, PrepStep.PREPARE), day.reason)
        assertTrue("efecto ${day.minutes}", day.minutes in 1..6)
        // Otro día de la semana no recibe el efecto.
        val tuesday = DurationEstimator.estimate(inputs(prepare = StepInputs(15, null, records)), DayOfWeek.TUESDAY)
        assertTrue(tuesday.adjustments.none { it.reason is AdjustmentReason.DayEffect })
    }

    @Test
    fun `el efecto del dia se limita a 10 minutos`() {
        val records = history(10, 10, 10, 10, 10, 10, 10) + history(60, 60, 60, day = monday)
        val estimate = DurationEstimator.estimate(inputs(prepare = StepInputs(15, null, records)), monday)
        assertEquals(10, estimate.adjustments.single { it.reason is AdjustmentReason.DayEffect }.minutes)
    }

    @Test
    fun `lluvia y trafico alto suman lo esperado`() {
        // Trayecto 20: lluvia 15 % = 3; tráfico alto 25 % = 5.
        val estimate = DurationEstimator.estimate(
            inputs(travel = StepInputs(20, null, emptyList())),
            monday,
            EstimationContext(rain = true, traffic = TrafficLevel.HIGH, place = "la universidad"),
        )
        assertEquals(28, estimate.durations.travel)
        assertTrue(Adjustment(AdjustmentReason.Rain, 3) in estimate.adjustments)
        assertTrue(Adjustment(AdjustmentReason.Traffic(TrafficLevel.HIGH, "la universidad"), 5) in estimate.adjustments)
    }

    @Test
    fun `lluvia suma al menos 2 minutos y trafico medio 10 por ciento`() {
        val estimate = DurationEstimator.estimate(
            inputs(travel = StepInputs(8, null, emptyList())),
            monday,
            EstimationContext(rain = true, traffic = TrafficLevel.MEDIUM),
        )
        assertEquals(8 + 2 + 1, estimate.durations.travel)
    }

    @Test
    fun `el contexto solo afecta el trayecto`() {
        val estimate = DurationEstimator.estimate(inputs(), monday, EstimationContext(rain = true, traffic = TrafficLevel.HIGH))
        assertEquals(StepDurations(10, 15, 7, 12 + 2 + 3), estimate.durations)
    }

    @Test
    fun `el ajuste manual reemplaza la base pero conserva dia y contexto`() {
        val records = history(15, 15, 15, 15) + history(21, 21, 21, day = monday)
        val estimate = DurationEstimator.estimate(
            inputs(prepare = StepInputs(15, manualMin = 10, history = records), travel = StepInputs(20, 25, emptyList())),
            monday,
            EstimationContext(rain = true),
        )
        val dayEffect = estimate.adjustments.single { it.reason is AdjustmentReason.DayEffect }.minutes
        assertEquals(10 + dayEffect, estimate.durations.prepare)
        assertEquals(25 + 4, estimate.durations.travel) // 15 % de 25 = 3,75 → 4
        assertTrue(estimate.adjustments.any { it.reason == AdjustmentReason.Manual(PrepStep.PREPARE, 10) })
        assertTrue(estimate.adjustments.none { it.reason is AdjustmentReason.Learned })
    }

    @Test
    fun `cada ajuste aplicado aparece en la explicacion y suman la diferencia`() {
        val records = history(15, 15, 15, 15) + history(21, 21, 21, day = monday)
        val estimate = DurationEstimator.estimate(
            inputs(prepare = StepInputs(15, null, records), travel = StepInputs(12, null, emptyList())),
            monday,
            EstimationContext(rain = true, traffic = TrafficLevel.MEDIUM),
        )
        val defaults = 10 + 15 + 7 + 12
        assertEquals(estimate.durations.total - defaults, estimate.adjustments.sumOf { it.minutes })
        val minutes = estimate.adjustments.map { it.minutes }
        assertEquals("ordenadas de mayor a menor", minutes.sortedDescending(), minutes)
    }
}
