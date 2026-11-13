package com.atiempo.wear.data.seed

import com.atiempo.wear.data.db.Event
import com.atiempo.wear.data.db.PrepSession
import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.data.db.StepRecord
import com.atiempo.wear.domain.model.PrepStep
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Datos de ejemplo para la demo (D-007): 3 rutinas, 3 eventos próximos y 14 días de historial.
 * Kotlin puro y determinista: con la misma fecha produce siempre lo mismo.
 *
 * El historial está armado para que las pantallas tengan qué mostrar:
 * - Los lunes, "Prepárate" de Clase tarda unos 6 min más, con 3 registros de lunes
 *   (el mínimo del modelo para aplicar el efecto del día).
 * - En los últimos 7 días hay 6 salidas, 5 a tiempo; el jueves salió tarde y el miércoles fue el
 *   mejor día (el de más margen), como en el resumen semanal del prototipo.
 */
object SampleData {

    const val CLASE_ID = 1L
    const val CITA_ID = 2L
    const val SOCIAL_ID = 3L

    /** Días de historial antes de hoy. */
    const val HISTORY_DAYS = 14

    /** Margen de seguridad usado para la hora de salida planeada del historial (MODELO_ESTIMACION §5). */
    private const val SAFETY_MARGIN_MIN = 3
    private const val MONDAY_PREPARE_EXTRA_MIN = 6
    private const val SEED = 42

    data class Seed(
        val routines: List<Routine>,
        val events: List<Event>,
        val sessions: List<PrepSession>,
        val records: List<StepRecord>,
    )

    fun routines(): List<Routine> = listOf(
        Routine(
            id = CLASE_ID, name = "Clase",
            items = listOf("Llaves", "Billetera", "Carné", "Cuaderno"),
            defaultFinishMin = 10, defaultPrepareMin = 15, defaultCheckMin = 7, defaultTravelMin = 12,
            manualCheckMin = 5,
        ),
        Routine(
            id = CITA_ID, name = "Cita",
            items = listOf("Llaves", "Billetera", "Documento", "Orden médica"),
            defaultFinishMin = 5, defaultPrepareMin = 15, defaultCheckMin = 5, defaultTravelMin = 20,
        ),
        Routine(
            id = SOCIAL_ID, name = "Salida social",
            items = listOf("Llaves", "Billetera", "Celular", "Chaqueta"),
            defaultFinishMin = 10, defaultPrepareMin = 30, defaultCheckMin = 5, defaultTravelMin = 25,
        ),
    )

    fun generate(now: LocalDateTime, zone: ZoneId): Seed {
        val random = Random(SEED)
        val routines = routines().associateBy { it.id }
        val events = mutableListOf<Event>()
        val sessions = mutableListOf<PrepSession>()
        val records = mutableListOf<StepRecord>()

        fun millis(at: LocalDateTime) = at.atZone(zone).toInstant().toEpochMilli()

        // Eventos próximos (ids 1 a 3), como en el prototipo. El primero, a poco más de una hora,
        // para que Inicio muestre una cuenta regresiva corta en la demo.
        val nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(2)
        events += Event(1, "Clase de cálculo", millis(nextHour), CLASE_ID, "Bloque 7", 12, createdByVoice = false)
        events += Event(2, "Reunión de grupo", millis(nextHour.plusHours(5)), CITA_ID, "Biblioteca", 10, createdByVoice = false)
        events += Event(
            3, "Cita médica", millis(now.toLocalDate().plusDays(3).atTime(15, 30)), CITA_ID,
            "Clínica del Norte", 20, createdByVoice = true,
        )

        val today = now.toLocalDate()
        for (daysAgo in HISTORY_DAYS downTo 1) {
            val date = today.minusDays(daysAgo.toLong())
            val olderWeek = daysAgo > 7
            planFor(date.dayOfWeek, olderWeek).forEach { past ->
                val eventId = events.size + 1L
                val routine = routines.getValue(past.routineId)
                val start = date.atTime(past.time)
                events += Event(eventId, past.title, millis(start), past.routineId, past.place, past.travelMin, createdByVoice = false)

                val sessionId = sessions.size + 1L
                val built = buildSession(sessionId, eventId, routine, date, start, past, random, ::millis)
                sessions += built.first
                records += built.second
            }
        }
        return Seed(routines.values.toList(), events, sessions, records)
    }

    /** Una salida pasada: qué evento era y con cuántos minutos de retraso (negativo = margen). */
    private data class PastOuting(
        val title: String,
        val routineId: Long,
        val time: LocalTime,
        val place: String,
        val travelMin: Int,
        val minutesLate: Int,
    )

    /**
     * Calendario semanal del historial. La semana reciente fija los resultados del resumen semanal;
     * la anterior suma una segunda clase el lunes para completar 3 registros de lunes.
     */
    private fun planFor(day: DayOfWeek, olderWeek: Boolean): List<PastOuting> {
        val clase = { late: Int -> PastOuting("Clase de cálculo", CLASE_ID, LocalTime.of(8, 0), "Bloque 7", 12, late) }
        return when (day) {
            DayOfWeek.MONDAY -> if (olderWeek) {
                listOf(clase(2), PastOuting("Laboratorio de física", CLASE_ID, LocalTime.of(14, 0), "Bloque 3", 12, -1))
            } else {
                listOf(clase(-3))
            }
            DayOfWeek.TUESDAY -> listOf(PastOuting("Reunión de grupo", CITA_ID, LocalTime.of(10, 0), "Biblioteca", 10, if (olderWeek) -4 else -5))
            DayOfWeek.WEDNESDAY -> listOf(clase(if (olderWeek) -2 else -8))
            DayOfWeek.THURSDAY -> listOf(clase(if (olderWeek) -3 else 4))
            DayOfWeek.FRIDAY -> listOf(clase(if (olderWeek) -1 else -2))
            DayOfWeek.SATURDAY -> listOf(PastOuting("Cine con amigos", SOCIAL_ID, LocalTime.of(19, 0), "Cine Andino", 25, if (olderWeek) 1 else -6))
            DayOfWeek.SUNDAY -> emptyList()
        }
    }

    /**
     * Construye la sesión y sus 4 registros. "Prepárate", "Revisa" y el trayecto tienen variación
     * aleatoria; "Termina" se ajusta para que la salida caiga exactamente con el retraso pedido.
     */
    private fun buildSession(
        sessionId: Long,
        eventId: Long,
        routine: Routine,
        date: LocalDate,
        start: LocalDateTime,
        past: PastOuting,
        random: Random,
        millis: (LocalDateTime) -> Long,
    ): Pair<PrepSession, List<StepRecord>> {
        val planned = listOf(
            routine.manualFinishMin ?: routine.defaultFinishMin,
            routine.manualPrepareMin ?: routine.defaultPrepareMin,
            routine.manualCheckMin ?: routine.defaultCheckMin,
            past.travelMin,
        )
        val plannedLeave = start.minusMinutes((past.travelMin + SAFETY_MARGIN_MIN).toLong())
        val startedAt = plannedLeave.minusMinutes((planned[0] + planned[1] + planned[2]).toLong())

        val mondayExtra = if (routine.id == CLASE_ID && date.dayOfWeek == DayOfWeek.MONDAY) MONDAY_PREPARE_EXTRA_MIN else 0
        // Tiempo total desde el primer aviso hasta salir, con el retraso pedido.
        val total = planned[0] + planned[1] + planned[2] + past.minutesLate
        val prepare = clampMin(planned[1] + mondayExtra + random.nextInt(-1, 3))
        val check = (planned[2] + random.nextInt(-1, 2)).coerceAtLeast(2)
        // "Termina" absorbe la diferencia: los días que prepararse toma más, se termina más rápido.
        val finish = clampMin(total - prepare - check)
        val travel = clampMin(planned[3] + random.nextInt(-2, 4))
        val leftAt = startedAt.plusMinutes((finish + prepare + check).toLong())
        val minutesLate = ChronoUnit.MINUTES.between(plannedLeave, leftAt).toInt()

        val session = PrepSession(sessionId, eventId, millis(startedAt), millis(leftAt), minutesLate)
        val actual = listOf(finish, prepare, check, travel)
        val records = PrepStep.entries.mapIndexed { i, step ->
            StepRecord(
                sessionId = sessionId,
                routineId = routine.id,
                step = step,
                dayOfWeek = date.dayOfWeek.value,
                plannedMin = planned[i],
                actualMin = actual[i],
            )
        }
        return session to records
    }

    /** Las duraciones reales se acotan entre 1 y 90 min (MODELO_ESTIMACION, Aprendizaje). */
    private fun clampMin(value: Int) = value.coerceIn(1, 90)
}
