package com.atiempo.wear.data.repo

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.domain.model.Adjustment
import com.atiempo.wear.domain.model.AdjustmentReason
import com.atiempo.wear.domain.model.PrepStep
import org.junit.Assert.assertEquals
import org.junit.Test

class ExplanationTest {
    private val event = Event(1, "Clase", 0L, routineId = 1, place = null, baseTravelMin = 12, createdByVoice = false)
    private fun routine(prepare: Int? = null, check: Int? = null, travel: Int? = null) = Routine(
        id = 1, name = "Clase", items = emptyList(),
        defaultFinishMin = 10, defaultPrepareMin = 15, defaultCheckMin = 7, defaultTravelMin = 12,
        manualPrepareMin = prepare, manualCheckMin = check, manualTravelMin = travel,
    )

    @Test
    fun `sin ajustes solo explica el margen`() {
        // 10 + 15 + 7 + 12 = 44 → margen 5
        assertEquals(listOf(Adjustment(AdjustmentReason.SafetyMargin, 5)), routine().explain(event))
    }

    @Test
    fun `cada ajuste manual aparece con su diferencia`() {
        val reasons = routine(prepare = 20, check = 5).explain(event)
        assertEquals(Adjustment(AdjustmentReason.Manual(PrepStep.PREPARE, 20), 5), reasons.first { it.reason is AdjustmentReason.Manual && (it.reason as AdjustmentReason.Manual).step == PrepStep.PREPARE })
        assertEquals(Adjustment(AdjustmentReason.Manual(PrepStep.CHECK, 5), -2), reasons.last())
    }

    @Test
    fun `ordenadas de mayor a menor efecto`() {
        val minutes = routine(prepare = 25, check = 5, travel = 15).explain(event).map { it.minutes }
        assertEquals(minutes.sortedDescending(), minutes)
    }

    @Test
    fun `un ajuste igual al valor por defecto no es una razon`() {
        assertEquals(1, routine(prepare = 15, travel = 12).explain(event).size)
    }
}
