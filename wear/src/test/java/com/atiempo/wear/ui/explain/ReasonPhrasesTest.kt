package com.atiempo.wear.ui.explain

import com.atiempo.wear.R
import com.atiempo.wear.domain.model.Adjustment
import com.atiempo.wear.domain.model.AdjustmentReason
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.domain.model.TrafficLevel
import com.atiempo.wear.ui.explain.PhraseArg.Minutes
import com.atiempo.wear.ui.explain.PhraseArg.Place
import com.atiempo.wear.ui.explain.PhraseArg.StepName
import com.atiempo.wear.ui.explain.PhraseArg.StepVerb
import com.atiempo.wear.ui.explain.PhraseArg.Weekday
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class ReasonPhrasesTest {

    private fun phrase(reason: AdjustmentReason, minutes: Int) = ReasonPhrases.reason(Adjustment(reason, minutes))

    @Test
    fun `lo aprendido dice cuanto tardas y en que`() {
        assertEquals(
            Phrase(R.string.explain_learned, listOf(Minutes(18), StepVerb(PrepStep.PREPARE))),
            phrase(AdjustmentReason.Learned(PrepStep.PREPARE, 18), 3),
        )
    }

    @Test
    fun `el dia lento dice cuantos minutos mas`() {
        assertEquals(
            Phrase(R.string.explain_day_more, listOf(Weekday(DayOfWeek.MONDAY), Minutes(6), StepVerb(PrepStep.PREPARE))),
            phrase(AdjustmentReason.DayEffect(DayOfWeek.MONDAY, PrepStep.PREPARE), 6),
        )
    }

    @Test
    fun `el dia rapido dice minutos menos sin cifra negativa`() {
        assertEquals(
            Phrase(R.string.explain_day_less, listOf(Weekday(DayOfWeek.FRIDAY), Minutes(4), StepVerb(PrepStep.FINISH))),
            phrase(AdjustmentReason.DayEffect(DayOfWeek.FRIDAY, PrepStep.FINISH), -4),
        )
    }

    @Test
    fun `el trafico usa su nivel y el lugar`() {
        assertEquals(
            Phrase(R.string.explain_traffic_high, listOf(Place("Biblioteca"))),
            phrase(AdjustmentReason.Traffic(TrafficLevel.HIGH, "Biblioteca"), 3),
        )
        assertEquals(
            Phrase(R.string.explain_traffic_medium, listOf(Place(null))),
            phrase(AdjustmentReason.Traffic(TrafficLevel.MEDIUM, null), 1),
        )
    }

    @Test
    fun `lluvia, ajuste manual y margen`() {
        assertEquals(Phrase(R.string.explain_rain), phrase(AdjustmentReason.Rain, 2))
        assertEquals(
            Phrase(R.string.explain_manual, listOf(StepName(PrepStep.LEAVE), Minutes(30))),
            phrase(AdjustmentReason.Manual(PrepStep.LEAVE, 30), 5),
        )
        assertEquals(Phrase(R.string.explain_margin), phrase(AdjustmentReason.SafetyMargin, 5))
    }

    @Test
    fun `el efecto lleva el signo`() {
        assertEquals(Phrase(R.string.explain_minutes_plus, listOf(Minutes(6))), ReasonPhrases.effect(6))
        assertEquals(Phrase(R.string.explain_minutes_minus, listOf(Minutes(2))), ReasonPhrases.effect(-2))
    }

    @Test
    fun `el total y el titulo siguen el signo`() {
        assertEquals(Phrase(R.string.explain_total_before, listOf(Minutes(13))), ReasonPhrases.total(13))
        assertEquals(Phrase(R.string.explain_total_after, listOf(Minutes(4))), ReasonPhrases.total(-4))
        assertEquals(Phrase(R.string.explain_total_same), ReasonPhrases.total(0))
        assertEquals(R.string.explain_title, ReasonPhrases.title(0))
        assertEquals(R.string.explain_title_later, ReasonPhrases.title(-4))
    }
}
