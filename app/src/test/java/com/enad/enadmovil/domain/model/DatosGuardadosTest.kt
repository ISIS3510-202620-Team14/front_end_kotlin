package com.enad.enadmovil.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class DatosGuardadosTest {

    private fun ms(texto: String) = ZonedDateTime.parse(texto).toInstant().toEpochMilli()

    private val ahora = ms("2026-10-29T10:00:00-05:00")

    @Test
    fun `guardado hoy en la madrugada`() {
        assertEquals("hoy a las 3:40 a. m.", DatosGuardados.cuando(ms("2026-10-29T03:40:00-05:00"), ahora))
    }

    @Test
    fun `guardado ayer a la una`() {
        assertEquals("ayer a la 1:05 p. m.", DatosGuardados.cuando(ms("2026-10-28T13:05:00-05:00"), ahora))
    }

    @Test
    fun `guardado otro dia muestra la fecha`() {
        assertEquals("el 3 de octubre a las 9:00 a. m.", DatosGuardados.cuando(ms("2026-10-03T09:00:00-05:00"), ahora))
    }

    @Test
    fun `medianoche y mediodia son las 12`() {
        assertEquals("hoy a las 12:00 a. m.", DatosGuardados.cuando(ms("2026-10-29T00:00:00-05:00"), ahora))
        assertEquals("ayer a las 12:30 p. m.", DatosGuardados.cuando(ms("2026-10-28T12:30:00-05:00"), ahora))
    }

    @Test
    fun `usa la hora de Colombia y no la UTC`() {
        // 02:00 UTC del 29 son las 9 p. m. del 28 en Bogotá.
        assertEquals("ayer a las 9:00 p. m.", DatosGuardados.cuando(ms("2026-10-29T02:00:00Z"), ahora))
    }
}