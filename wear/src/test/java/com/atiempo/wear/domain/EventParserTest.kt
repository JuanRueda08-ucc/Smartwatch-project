package com.atiempo.wear.domain

import com.atiempo.wear.domain.model.RoutineKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class EventParserTest {
    // Lunes 16 de noviembre de 2026, 9:00
    private val now = LocalDateTime.of(2026, 11, 16, 9, 0)
    private val monday = LocalDate.of(2026, 11, 16)
    private fun parse(text: String) = EventParser.parse(text, now)

    @Test
    fun `ejemplo de la pantalla - clase manana a las 7`() {
        val e = parse("Clase de cálculo mañana a las 7")
        assertEquals("Clase de cálculo", e.title.value)
        assertEquals(monday.plusDays(1), e.date.value)
        assertEquals(LocalTime.of(7, 0), e.time.value)
        assertFalse("sin mañana/tarde la hora es dudosa", e.time.confident)
        assertEquals(RoutineKind.CLASS, e.routine.value)
        assertNull(e.place.value)
        assertFalse(e.place.confident)
    }

    @Test
    fun `ejemplo de la pantalla - cita el jueves a las 3 y media`() {
        val e = parse("Cita médica el jueves a las 3 y media")
        assertEquals("Cita médica", e.title.value)
        assertEquals(LocalDate.of(2026, 11, 19), e.date.value)
        assertEquals(LocalTime.of(15, 30), e.time.value) // de 1 a 6 se asume tarde
        assertEquals(RoutineKind.APPOINTMENT, e.routine.value)
    }

    @Test
    fun `hora con periodo explicito es segura`() {
        val e = parse("Cine con Laura el sábado a las 6 de la tarde")
        assertEquals("Cine con Laura", e.title.value)
        assertEquals(LocalDate.of(2026, 11, 21), e.date.value)
        assertEquals(LocalTime.of(18, 0), e.time.value)
        assertTrue(e.time.confident)
        assertEquals(RoutineKind.SOCIAL, e.routine.value)
    }

    @Test
    fun `hora de 24 horas y lugar al final`() {
        val e = parse("Reunión de grupo hoy a las 15:30 en la biblioteca")
        assertEquals("Reunión de grupo", e.title.value)
        assertEquals(monday, e.date.value)
        assertEquals(LocalTime.of(15, 30), e.time.value)
        assertTrue(e.time.confident)
        assertEquals("Biblioteca", e.place.value)
        assertTrue(e.place.confident)
    }

    @Test
    fun `lugar en medio de la frase`() {
        val e = parse("Parcial de física en el bloque 7 pasado mañana a las 8 de la mañana")
        assertEquals("Parcial de física", e.title.value)
        assertEquals("Bloque 7", e.place.value)
        assertEquals(monday.plusDays(2), e.date.value)
        assertEquals(LocalTime.of(8, 0), e.time.value)
        assertEquals(RoutineKind.CLASS, e.routine.value)
    }

    @Test
    fun `mediodia y numeros en palabras`() {
        assertEquals(LocalTime.NOON, parse("Almuerzo con mamá mañana al mediodía").time.value)
        assertEquals(LocalTime.of(13, 0), parse("Clase de inglés a la una").time.value)
        assertEquals(LocalTime.of(21, 0), parse("Fiesta de Ana el viernes a las nueve de la noche").time.value)
    }

    @Test
    fun `cuarto y menos cuarto`() {
        assertEquals(LocalTime.of(10, 15), parse("Cita a las 10 y cuarto de la mañana").time.value)
        assertEquals(LocalTime.of(16, 45), parse("Cita a las 5 menos cuarto").time.value)
    }

    @Test
    fun `am y pm`() {
        assertEquals(LocalTime.of(20, 0), parse("Cena a las 8 pm").time.value)
        assertEquals(LocalTime.of(8, 0), parse("Clase a las 8 a. m.").time.value)
    }

    @Test
    fun `de la manana no se confunde con manana el dia`() {
        val e = parse("Clase el miércoles a las 7 de la mañana")
        assertEquals(LocalDate.of(2026, 11, 18), e.date.value)
        assertEquals(LocalTime.of(7, 0), e.time.value)
    }

    @Test
    fun `el mismo dia de la semana es la semana siguiente`() {
        assertEquals(monday.plusDays(7), parse("Clase el lunes a las 10 de la mañana").date.value)
    }

    @Test
    fun `sin dia es hoy si la hora no ha pasado y si no manana`() {
        val later = parse("Cita a las 15:00")
        assertEquals(monday, later.date.value)
        assertFalse(later.date.confident)
        assertEquals(monday.plusDays(1), parse("Cita a las 7 de la mañana").date.value)
    }

    @Test
    fun `quita muletillas del inicio`() {
        assertEquals("Cita con el odontólogo", parse("Tengo cita con el odontólogo el martes a las 4").title.value)
        assertEquals("Cine", parse("Voy a ir a cine el sábado a las 7 de la noche").title.value)
    }

    @Test
    fun `sin hora ni rutina todo queda marcado como dudoso`() {
        val e = parse("Ver a Juan")
        assertEquals("Ver a Juan", e.title.value)
        assertFalse(e.time.confident)
        assertFalse(e.date.confident)
        assertFalse(e.routine.confident)
    }

    @Test
    fun `frase vacia usa un titulo por defecto`() {
        val e = parse("mañana a las 9 de la mañana")
        assertEquals("Cita", e.title.value)
        assertFalse(e.title.confident)
    }

    @Test
    fun `plegado conserva la longitud`() {
        val text = "Reunión mañana en el Café Árbol"
        assertEquals(text.length, EventParser.fold(text).length)
        assertEquals("reunion manana en el cafe arbol", EventParser.fold(text))
    }
}
