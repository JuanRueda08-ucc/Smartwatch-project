package com.atiempo.wear.domain.model

import java.time.DayOfWeek

/** Nivel de tráfico del proveedor de contexto (simulado, D-003). */
enum class TrafficLevel { LOW, MEDIUM, HIGH }

/**
 * Por qué cambió la hora de un aviso (MODELO_ESTIMACION.md, Explicación). Son datos, no textos:
 * la pantalla arma cada frase desde `strings.xml`.
 */
sealed interface AdjustmentReason {
    /** "Últimamente tardas 18 min en prepararte": la base aprendida difiere del valor por defecto. */
    data class Learned(val step: PrepStep, val learnedMin: Int) : AdjustmentReason

    /** "Los lunes tardas 6 min más en prepararte". */
    data class DayEffect(val day: DayOfWeek, val step: PrepStep) : AdjustmentReason

    /** "Hay lluvia prevista". */
    data object Rain : AdjustmentReason

    /** "Tráfico alto hacia la Biblioteca". */
    data class Traffic(val level: TrafficLevel, val place: String?) : AdjustmentReason

    /** "Tú ajustaste «Revisa tus cosas» a 5 min". */
    data class Manual(val step: PrepStep, val manualMin: Int) : AdjustmentReason

    /** "Margen para llegar con calma". */
    data object SafetyMargin : AdjustmentReason
}

/** Un ajuste y su efecto en minutos: positivo = te aviso antes; negativo = después. */
data class Adjustment(val reason: AdjustmentReason, val minutes: Int)
