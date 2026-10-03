package com.enad.enadmovil.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecomendadorAgrupacionTest {

    private fun evento(materia: String, tamano: Int, metodo: MetodoAgrupacion) = EventoAgrupacion(materia, tamano, metodo)

    @Test
    fun sinEventosRecomiendaAutomaticoSinDatosSuficientes() {
        val r = RecomendadorAgrupacion.recomendar(emptyList(), "matematicas", 12)
        assertEquals(MetodoAgrupacion.AUTOMATICO, r.metodo)
        assertEquals(0.5, r.confianza, 0.0)
        assertEquals(0, r.muestra)
        assertFalse(r.tieneDatosSuficientes)
    }

    @Test
    fun recomiendaElMetodoMasUsadoEnSalonesParecidos() {
        val eventos = listOf(
            evento("matematicas", 12, MetodoAgrupacion.MANUAL),
            evento("matematicas", 14, MetodoAgrupacion.MANUAL),
            evento("matematicas", 10, MetodoAgrupacion.MANUAL),
            evento("matematicas", 13, MetodoAgrupacion.AUTOMATICO)
        )
        val r = RecomendadorAgrupacion.recomendar(eventos, "matematicas", 12)
        assertEquals(MetodoAgrupacion.MANUAL, r.metodo)
        assertEquals(0.75, r.confianza, 0.0)
        assertEquals(4, r.muestra)
        assertTrue(r.tieneDatosSuficientes)
    }

    @Test
    fun ignoraOtrasMateriasYTamanosLejanos() {
        val eventos = listOf(
            evento("lectura", 12, MetodoAgrupacion.MANUAL),
            evento("matematicas", 30, MetodoAgrupacion.MANUAL),
            evento("matematicas", 12, MetodoAgrupacion.AUTOMATICO)
        )
        val r = RecomendadorAgrupacion.recomendar(eventos, "matematicas", 12)
        assertEquals(MetodoAgrupacion.AUTOMATICO, r.metodo)
        assertEquals(1, r.muestra)
        assertFalse(r.tieneDatosSuficientes)
    }

    @Test
    fun conEmpateGanaAutomatico() {
        val eventos = listOf(
            evento("lectura", 8, MetodoAgrupacion.AUTOMATICO),
            evento("lectura", 8, MetodoAgrupacion.MANUAL)
        )
        val r = RecomendadorAgrupacion.recomendar(eventos, "lectura", 8)
        assertEquals(MetodoAgrupacion.AUTOMATICO, r.metodo)
        assertEquals(0.5, r.confianza, 0.0)
    }

    @Test
    fun laToleranciaCreceConElTamanoDelSalon() {
        assertEquals(3, RecomendadorAgrupacion.toleranciaDeTamano(10))
        assertEquals(5, RecomendadorAgrupacion.toleranciaDeTamano(11))
        assertEquals(5, RecomendadorAgrupacion.toleranciaDeTamano(25))
        assertEquals(8, RecomendadorAgrupacion.toleranciaDeTamano(26))
    }

    @Test
    fun laClaveDelBackendSeConvierteAlMetodo() {
        assertEquals(MetodoAgrupacion.AUTOMATICO, MetodoAgrupacion.deClave("automatic"))
        assertEquals(MetodoAgrupacion.MANUAL, MetodoAgrupacion.deClave("manual"))
        assertEquals(null, MetodoAgrupacion.deClave("otro"))
    }
}
