package com.atiempo.wear.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** Tipo de rutina que el intérprete deduce de la frase. Corresponde a las 3 rutinas sembradas. */
enum class RoutineKind { CLASS, APPOINTMENT, SOCIAL }

/**
 * Un dato interpretado y si el intérprete está seguro de él. Los dudosos se resaltan en la pantalla
 * de confirmación con "Revisa esto" (DISENO_UI §3.5).
 */
data class Parsed<T>(val value: T, val confident: Boolean)

/** Resultado de interpretar una frase como "Cita médica el jueves a las 3 y media". */
data class ParsedEvent(
    val transcript: String,
    val title: Parsed<String>,
    val date: Parsed<LocalDate>,
    val time: Parsed<LocalTime>,
    val routine: Parsed<RoutineKind>,
    /** null = la frase no dice dónde. */
    val place: Parsed<String?>,
)
