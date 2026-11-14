package com.atiempo.wear.domain

import com.atiempo.wear.domain.model.Parsed
import com.atiempo.wear.domain.model.ParsedEvent
import com.atiempo.wear.domain.model.RoutineKind
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Intérprete por reglas de frases en español (ARQUITECTURA.md, D-006). Sin servicios externos:
 * funciona sin conexión y es predecible en la demo.
 *
 * Reconoce:
 * - Días: hoy, mañana, pasado mañana, lunes…domingo (con o sin "el", "este", "próximo").
 * - Horas: "a las 7", "a la una", "a las 3 y media", "a las 15:30", "7 de la mañana/tarde/noche",
 *   "al mediodía", "a las 8 pm".
 * - Rutina por palabras clave (clase/parcial → Clase; cita/médico/reunión → Cita; cine/fiesta → Salida social).
 * - Lugar: lo que sigue a "en".
 * - Título: lo que queda de la frase.
 *
 * Trabaja sobre una versión en minúsculas y sin tildes con la misma longitud que el original, para
 * poder recortar del texto original las partes ya interpretadas.
 */
object EventParser {

    private val NUMBER_WORDS = mapOf(
        "una" to 1, "un" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "seis" to 6,
        "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12,
    )
    private val WEEKDAYS = mapOf(
        "lunes" to DayOfWeek.MONDAY, "martes" to DayOfWeek.TUESDAY, "miercoles" to DayOfWeek.WEDNESDAY,
        "jueves" to DayOfWeek.THURSDAY, "viernes" to DayOfWeek.FRIDAY, "sabado" to DayOfWeek.SATURDAY,
        "domingo" to DayOfWeek.SUNDAY,
    )
    private val ROUTINE_KEYWORDS = listOf(
        RoutineKind.CLASS to listOf("clase", "parcial", "examen", "quiz", "universidad", "laboratorio", "taller", "monitoria"),
        RoutineKind.APPOINTMENT to listOf("cita", "medico", "medica", "doctor", "odontologo", "dentista", "reunion", "entrevista", "tramite"),
        RoutineKind.SOCIAL to listOf("cine", "fiesta", "comida", "almuerzo", "cena", "cumpleanos", "salida", "partido", "concierto", "cafe"),
    )

    private const val HOUR = "(\\d{1,2}|una|un|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce)"
    private const val PERIOD = "(?:\\s+(?:de|en|por)\\s+la\\s+(manana|tarde|noche)|\\s*(a\\.?\\s?m\\.?|p\\.?\\s?m\\.?)(?=\\s|$))?"
    private const val FRACTION = "(?::(\\d{2})|\\s+y\\s+(media|cuarto|\\d{1,2})|\\s+menos\\s+(cuarto|\\d{1,2}))?"

    /** "a las 3 y media de la tarde", "para las 15:30", "a la una". */
    private val TIME_AFTER_A_LAS = Regex("\\b(?:a|para|como a|tipo)\\s+las?\\s+$HOUR$FRACTION$PERIOD")
    /** "7 de la mañana" sin "a las". */
    private val TIME_WITH_PERIOD = Regex("\\b$HOUR$FRACTION\\s+(?:de|en|por)\\s+la\\s+(manana|tarde|noche)")
    private val NOON = Regex("\\b(?:al|a)\\s+mediodia\\b")
    private val MIDNIGHT = Regex("\\ba\\s+(?:la\\s+)?medianoche\\b")

    private val DAY_AFTER_TOMORROW = Regex("\\bpasado\\s+manana\\b")
    private val TOMORROW = Regex("\\bmanana\\b")
    private val TODAY = Regex("\\b(?:para\\s+)?hoy\\b")
    private val WEEKDAY = Regex("\\b(?:el\\s+|este\\s+|el\\s+proximo\\s+|este\\s+proximo\\s+|proximo\\s+)?(lunes|martes|miercoles|jueves|viernes|sabado|domingo)\\b")

    private val PLACE = Regex("\\ben\\s+")
    private val LEADING_ARTICLE = Regex("^(?:el|la|los|las)\\s+", RegexOption.IGNORE_CASE)
    private val LEADING_FILLER = Regex(
        "^(?:recuerdame\\s+(?:que\\s+)?(?:tengo\\s+)?|tengo\\s+que\\s+ir\\s+a\\s+|tengo\\s+(?:una?\\s+)?|voy\\s+a\\s+(?:ir\\s+a\\s+)?|hay\\s+(?:una?\\s+)?)",
    )
    private val TRAILING_CONNECTOR = Regex("(?:\\s+(?:el|la|los|las|de|del|para|a|al|y|este|con|en))+$")

    fun parse(transcript: String, now: LocalDateTime): ParsedEvent {
        val original = transcript.trim().replace(Regex("\\s+"), " ")
        val norm = fold(original)
        val taken = mutableListOf<IntRange>()

        val time = findTime(norm, taken)
        val dateFound = findDate(norm, taken, now.toLocalDate())
        // Sin día explícito: hoy si la hora aún no pasa, si no mañana.
        val date = dateFound ?: run {
            val today = now.toLocalDate()
            val day = if (time != null && time.value.isBefore(now.toLocalTime())) today.plusDays(1) else today
            Parsed(day, confident = false)
        }
        val place = findPlace(original, norm, taken)
        val routine = findRoutine(norm)
        val title = findTitle(original, taken, routine.value)

        return ParsedEvent(
            transcript = original,
            title = title,
            date = date,
            time = time ?: Parsed(LocalTime.of(9, 0), confident = false),
            routine = routine,
            place = place,
        )
    }

    // ---------- Hora ----------

    private fun findTime(norm: String, taken: MutableList<IntRange>): Parsed<LocalTime>? {
        NOON.find(norm)?.let { taken += it.range; return Parsed(LocalTime.NOON, true) }
        MIDNIGHT.find(norm)?.let { taken += it.range; return Parsed(LocalTime.MIDNIGHT, true) }
        val match = TIME_AFTER_A_LAS.find(norm) ?: TIME_WITH_PERIOD.find(norm) ?: return null
        taken += match.range
        val g = match.groupValues
        val hour = g[1].toIntOrNull() ?: NUMBER_WORDS.getValue(g[1])
        val colonMinutes = g[2]
        val plus = g[3]
        val minus = g[4]
        // En TIME_AFTER_A_LAS el periodo está en los grupos 5 (palabra) o 6 (am/pm); en TIME_WITH_PERIOD, en el 5.
        val periodWord = g.getOrElse(5) { "" }
        val ampm = g.getOrElse(6) { "" }.replace(Regex("[.\\s]"), "")

        var h = hour
        var m = when {
            colonMinutes.isNotEmpty() -> colonMinutes.toInt()
            plus == "media" -> 30
            plus == "cuarto" -> 15
            plus.isNotEmpty() -> plus.toInt()
            else -> 0
        }
        if (minus.isNotEmpty()) {
            h -= 1
            m = 60 - (if (minus == "cuarto") 15 else minus.toInt())
        }
        if (h !in 0..23 || m !in 0..59) return null

        val explicit = periodWord.isNotEmpty() || ampm.isNotEmpty() || h >= 13 || h == 0
        when {
            periodWord == "tarde" || periodWord == "noche" || ampm == "pm" -> if (h < 12) h += 12
            periodWord == "manana" || ampm == "am" -> if (h == 12) h = 0
            // Sin periodo: de 1 a 6 se asume tarde ("a las 3" casi nunca es de madrugada).
            !explicit && h in 1..6 -> h += 12
        }
        return Parsed(LocalTime.of(h % 24, m), confident = explicit || colonMinutes.isNotEmpty())
    }

    // ---------- Día ----------

    private fun findDate(norm: String, taken: MutableList<IntRange>, today: LocalDate): Parsed<LocalDate>? {
        fun free(match: MatchResult) = taken.none { it.first <= match.range.last && match.range.first <= it.last }
        DAY_AFTER_TOMORROW.findAll(norm).firstOrNull(::free)?.let { taken += it.range; return Parsed(today.plusDays(2), true) }
        TOMORROW.findAll(norm).firstOrNull(::free)?.let { taken += it.range; return Parsed(today.plusDays(1), true) }
        TODAY.findAll(norm).firstOrNull(::free)?.let { taken += it.range; return Parsed(today, true) }
        WEEKDAY.findAll(norm).firstOrNull(::free)?.let { match ->
            taken += match.range
            val target = WEEKDAYS.getValue(match.groupValues[1])
            // El mismo día de la semana que hoy se entiende como la semana siguiente.
            var days = (target.value - today.dayOfWeek.value + 7) % 7
            if (days == 0) days = 7
            return Parsed(today.plusDays(days.toLong()), true)
        }
        return null
    }

    // ---------- Lugar ----------

    private fun findPlace(original: String, norm: String, taken: MutableList<IntRange>): Parsed<String?> {
        val start = PLACE.findAll(norm).firstOrNull { m -> taken.none { m.range.first in it } } ?: return Parsed(null, false)
        val from = start.range.last + 1
        // El lugar llega hasta el siguiente dato ya interpretado (día u hora) o hasta el final.
        val end = taken.filter { it.first > from }.minOfOrNull { it.first } ?: original.length
        val raw = original.substring(from, end).trim().trimEnd(',', '.')
        if (raw.isEmpty()) return Parsed(null, false)
        taken += start.range.first until end
        val place = raw.replace(LEADING_ARTICLE, "").replaceFirstChar { it.uppercase() }
        return Parsed(place, true)
    }

    // ---------- Rutina ----------

    private fun findRoutine(norm: String): Parsed<RoutineKind> {
        val words = norm.split(Regex("[^a-z0-9]+")).toSet()
        ROUTINE_KEYWORDS.forEach { (kind, keywords) ->
            if (keywords.any { it in words }) return Parsed(kind, true)
        }
        return Parsed(RoutineKind.APPOINTMENT, false)
    }

    // ---------- Título ----------

    private fun findTitle(original: String, taken: List<IntRange>, routine: RoutineKind): Parsed<String> {
        val kept = StringBuilder()
        original.forEachIndexed { i, c -> kept.append(if (taken.any { i in it }) ' ' else c) }
        var title = kept.toString().replace(Regex("\\s+"), " ").trim().trimEnd(',', '.')
        val folded = fold(title)
        LEADING_FILLER.find(folded)?.let { title = title.substring(it.range.last + 1) }
        // Quita conectores sueltos al final ("Cita médica el" → "Cita médica").
        while (true) {
            val match = TRAILING_CONNECTOR.find(fold(title)) ?: break
            title = title.substring(0, match.range.first)
        }
        title = title.trim()
        if (title.isEmpty()) {
            val fallback = when (routine) {
                RoutineKind.CLASS -> "Clase"
                RoutineKind.APPOINTMENT -> "Cita"
                RoutineKind.SOCIAL -> "Salida"
            }
            return Parsed(fallback, confident = false)
        }
        return Parsed(title.replaceFirstChar { it.uppercase() }, confident = true)
    }

    /** Minúsculas y sin tildes, carácter por carácter: conserva la longitud del texto original. */
    internal fun fold(text: String): String = buildString(text.length) {
        text.forEach { c ->
            append(
                when (val l = c.lowercaseChar()) {
                    'á', 'à', 'ä' -> 'a'
                    'é', 'è', 'ë' -> 'e'
                    'í', 'ì', 'ï' -> 'i'
                    'ó', 'ò', 'ö' -> 'o'
                    'ú', 'ù', 'ü' -> 'u'
                    'ñ' -> 'n'
                    else -> l
                },
            )
        }
    }
}
