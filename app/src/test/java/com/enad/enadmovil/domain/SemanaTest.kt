package com.enad.enadmovil.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SemanaTest {

    @Test
    fun `las semanas anteriores van de la mas antigua a la mas reciente`() {
        val hoy = LocalDate.of(2026, 10, 30)
        assertEquals(
            listOf("2026-10-05", "2026-10-12", "2026-10-19"),
            Semana.anteriores(3, hoy)
        )
    }

    @Test
    fun `la semana en curso no cuenta si hoy es lunes`() {
        val hoy = LocalDate.of(2026, 10, 26)
        assertEquals(listOf("2026-10-19"), Semana.anteriores(1, hoy))
    }

    @Test
    fun `la semana en curso no cuenta si hoy es domingo`() {
        val hoy = LocalDate.of(2026, 11, 1)
        assertEquals(listOf("2026-10-19"), Semana.anteriores(1, hoy))
    }

    @Test
    fun `cruza el cambio de anio`() {
        val hoy = LocalDate.of(2027, 1, 5)
        assertEquals(listOf("2026-12-21", "2026-12-28"), Semana.anteriores(2, hoy))
    }

    @Test
    fun `sin semanas pedidas la lista queda vacia`() {
        assertTrue(Semana.anteriores(0, LocalDate.of(2026, 10, 30)).isEmpty())
    }
}