package com.enad.enadmovil.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UltimaEvaluacionTest {

    private val hoy = LocalDate.of(2026, 10, 28)

    @Test
    fun `sin fecha no hay dias y queda pendiente`() {
        assertNull(UltimaEvaluacion.diasDesde(null, hoy))
        assertTrue(UltimaEvaluacion.pendiente(null))
    }

    @Test
    fun `cuenta los dias desde la fecha del backend`() {
        assertEquals(10, UltimaEvaluacion.diasDesde("2026-10-18T15:00:00.000Z", hoy))
    }

    @Test
    fun `la fecha se cuenta en hora de Colombia`() {
        // 03:00 UTC del 28 son las 22:00 del 27 en Colombia: fue ayer
        assertEquals(1, UltimaEvaluacion.diasDesde("2026-10-28T03:00:00.000Z", hoy))
    }

    @Test
    fun `hasta el limite no esta pendiente y despues si`() {
        assertFalse(UltimaEvaluacion.pendiente(UltimaEvaluacion.DIAS_LIMITE))
        assertTrue(UltimaEvaluacion.pendiente(UltimaEvaluacion.DIAS_LIMITE + 1))
    }

    @Test
    fun `una fecha futura cuenta como hoy y una invalida como sin fecha`() {
        assertEquals(0, UltimaEvaluacion.diasDesde("2026-11-02T10:00:00.000Z", hoy))
        assertNull(UltimaEvaluacion.diasDesde("no es fecha", hoy))
    }

    @Test
    fun `texto corto para la ficha`() {
        assertEquals("evaluado hoy", UltimaEvaluacion.texto(0))
        assertEquals("evaluado ayer", UltimaEvaluacion.texto(1))
        assertEquals("hace 45 días", UltimaEvaluacion.texto(45))
    }

    @Test
    fun `cuenta solo a quien lleva mas dias que el limite`() {
        val fechas = listOf(
            "2026-08-01T10:00:00.000Z",   // 88 días: atrasado
            "2026-09-28T10:00:00.000Z",   // 30 días: justo en el límite, no cuenta
            "2026-10-20T10:00:00.000Z",   // 8 días: al día
            "2026-09-01T10:00:00.000Z"    // 57 días: atrasado
        )
        assertEquals(2, UltimaEvaluacion.atrasados(fechas, hoy))
    }

    @Test
    fun `quien nunca se evaluo o tiene una fecha invalida no cuenta como atrasado`() {
        assertEquals(0, UltimaEvaluacion.atrasados(listOf(null, "", "no es fecha"), hoy))
        assertEquals(0, UltimaEvaluacion.atrasados(emptyList(), hoy))
    }
}