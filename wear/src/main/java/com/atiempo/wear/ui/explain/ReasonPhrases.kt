package com.atiempo.wear.ui.explain

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.atiempo.wear.R
import com.atiempo.wear.domain.model.Adjustment
import com.atiempo.wear.domain.model.AdjustmentReason
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.domain.model.TrafficLevel
import java.time.DayOfWeek
import kotlin.math.abs

/** Una pieza variable de una frase; se resuelve con `strings.xml` al mostrarla. */
sealed interface PhraseArg {
    data class Minutes(val value: Int) : PhraseArg
    /** "prepararte", "llegar"… */
    data class StepVerb(val step: PrepStep) : PhraseArg
    /** "Prepárate", "Trayecto"… */
    data class StepName(val step: PrepStep) : PhraseArg
    /** "lunes", "sábados"… */
    data class Weekday(val day: DayOfWeek) : PhraseArg
    /** El lugar del evento, o "tu destino" si no tiene. */
    data class Place(val name: String?) : PhraseArg
}

/** Una frase de la explicación: el texto de `strings.xml` y sus argumentos, en orden. */
data class Phrase(@StringRes val text: Int, val args: List<PhraseArg> = emptyList())

/**
 * Genera el texto de la explicación (tarea 4.3, MODELO_ESTIMACION "Explicación"). Lógica pura: decide qué frase y
 * con qué datos; la pantalla solo la resuelve. Siempre en términos de la persona, sin cifras negativas en la frase:
 * el signo va en el efecto ("+6 min" / "−2 min").
 */
object ReasonPhrases {

    fun reason(adjustment: Adjustment): Phrase = when (val reason = adjustment.reason) {
        is AdjustmentReason.Learned ->
            Phrase(R.string.explain_learned, listOf(PhraseArg.Minutes(reason.learnedMin), PhraseArg.StepVerb(reason.step)))
        is AdjustmentReason.DayEffect -> Phrase(
            if (adjustment.minutes >= 0) R.string.explain_day_more else R.string.explain_day_less,
            listOf(PhraseArg.Weekday(reason.day), PhraseArg.Minutes(abs(adjustment.minutes)), PhraseArg.StepVerb(reason.step)),
        )
        AdjustmentReason.Rain -> Phrase(R.string.explain_rain)
        is AdjustmentReason.Traffic -> Phrase(
            if (reason.level == TrafficLevel.HIGH) R.string.explain_traffic_high else R.string.explain_traffic_medium,
            listOf(PhraseArg.Place(reason.place)),
        )
        is AdjustmentReason.Manual ->
            Phrase(R.string.explain_manual, listOf(PhraseArg.StepName(reason.step), PhraseArg.Minutes(reason.manualMin)))
        AdjustmentReason.SafetyMargin -> Phrase(R.string.explain_margin)
    }

    /** El efecto de una razón: "+6 min" o "−2 min". */
    fun effect(minutes: Int): Phrase =
        Phrase(if (minutes >= 0) R.string.explain_minutes_plus else R.string.explain_minutes_minus, listOf(PhraseArg.Minutes(abs(minutes))))

    /** El total debajo de la lista, contra la rutina sin ajustes. */
    fun total(minutes: Int): Phrase = when {
        minutes > 0 -> Phrase(R.string.explain_total_before, listOf(PhraseArg.Minutes(minutes)))
        minutes < 0 -> Phrase(R.string.explain_total_after, listOf(PhraseArg.Minutes(abs(minutes))))
        else -> Phrase(R.string.explain_total_same)
    }

    /** El título sigue al total: no dice "antes" si en realidad te aviso más tarde. */
    @StringRes
    fun title(totalMinutes: Int): Int =
        if (totalMinutes >= 0) R.string.explain_title else R.string.explain_title_later
}

/** Resuelve una frase con los textos en español. */
@Composable
fun Phrase.resolve(): String {
    val weekdays = stringArrayResource(R.array.weekdays_plural)
    val args = args.map<PhraseArg, Any> { arg ->
        when (arg) {
            is PhraseArg.Minutes -> arg.value
            is PhraseArg.StepVerb -> stringResource(stepVerbRes(arg.step))
            is PhraseArg.StepName -> stringResource(stepNameRes(arg.step))
            is PhraseArg.Weekday -> weekdays[arg.day.value - 1]
            is PhraseArg.Place -> arg.name ?: stringResource(R.string.explain_traffic_somewhere)
        }
    }
    return stringResource(text, *args.toTypedArray())
}

@StringRes
private fun stepVerbRes(step: PrepStep): Int = when (step) {
    PrepStep.FINISH -> R.string.step_verb_finish
    PrepStep.PREPARE -> R.string.step_verb_prepare
    PrepStep.CHECK -> R.string.step_verb_check
    PrepStep.LEAVE -> R.string.step_verb_leave
}

/** Nombre del paso tal como se ajusta: el último es el trayecto. */
@StringRes
private fun stepNameRes(step: PrepStep): Int = when (step) {
    PrepStep.FINISH -> R.string.step_finish
    PrepStep.PREPARE -> R.string.step_prepare
    PrepStep.CHECK -> R.string.step_check
    PrepStep.LEAVE -> R.string.step_travel
}
