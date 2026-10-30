package com.enad.enadmovil.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiesgoAbandonoTest {

    @Test
    fun `con menos de cuatro semanas no se predice nada`() {
        assertEquals(RiesgoAbandono.SIN_RIESGO, RiesgoAbandono.desdeAperturas(emptyList()))
        assertEquals(RiesgoAbandono.SIN_RIESGO, RiesgoAbandono.desdeAperturas(listOf(6, 5, 0)))
    }

    @Test
    fun `hay riesgo cuando la semana pasada cae a menos de la mitad del habito`() {
        val riesgo = RiesgoAbandono.desdeAperturas(listOf(6, 5, 7, 2))
        assertTrue(riesgo.enRiesgo)
        assertEquals(2, riesgo.semanaPasada)
        assertEquals(6.0, riesgo.semanaHabitual, 0.0001)
    }

    @Test
    fun `no hay riesgo si la caida es menor a la mitad`() {
        assertFalse(RiesgoAbandono.desdeAperturas(listOf(6, 5, 7, 4)).enRiesgo)
    }

    @Test
    fun `justo la mitad del habito ya cuenta como riesgo`() {
        assertTrue(RiesgoAbandono.desdeAperturas(listOf(4, 4, 4, 2)).enRiesgo)
    }

    @Test
    fun `sin habito previo no hay riesgo aunque no abra la app`() {
        val riesgo = RiesgoAbandono.desdeAperturas(listOf(0, 1, 0, 0))
        assertFalse(riesgo.enRiesgo)
        assertEquals(0, riesgo.semanaPasada)
    }

    @Test
    fun `un habito de una vez por semana que se pierde por completo es riesgo`() {
        assertTrue(RiesgoAbandono.desdeAperturas(listOf(1, 1, 1, 0)).enRiesgo)
    }

    @Test
    fun `solo cuentan las ultimas cuatro semanas`() {
        val riesgo = RiesgoAbandono.desdeAperturas(listOf(1, 1, 1, 1, 6, 5, 7, 2))
        assertTrue(riesgo.enRiesgo)
        assertEquals(6.0, riesgo.semanaHabitual, 0.0001)
    }

    @Test
    fun `volver a usar la app mas que antes no es riesgo`() {
        assertFalse(RiesgoAbandono.desdeAperturas(listOf(2, 3, 2, 9)).enRiesgo)
    }
}