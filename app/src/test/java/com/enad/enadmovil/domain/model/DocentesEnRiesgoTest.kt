package com.enad.enadmovil.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocentesEnRiesgoTest {

    private fun semana(vararg docentes: Pair<String, Int>) = docentes.map { AperturasDocente(it.first, "Docente ${it.first}", it.second) }

    @Test
    fun `solo queda el docente que bajo a la mitad o menos`() {
        val r = DocentesEnRiesgo.calcular(
            "2026-11-16",
            listOf(semana("a" to 6, "b" to 4), semana("a" to 5, "b" to 4), semana("a" to 7, "b" to 4), semana("a" to 2, "b" to 3))
        )
        assertEquals("2026-11-16", r.ultimaSemana)
        assertEquals(listOf("a"), r.docentes.map { it.uid })
        assertEquals(2, r.docentes[0].semanaPasada)
        assertEquals(6.0, r.docentes[0].semanaHabitual, 0.001)
    }

    @Test
    fun `quien no aparece en una semana abrio la app cero veces`() {
        val r = DocentesEnRiesgo.calcular("2026-11-16", listOf(semana("a" to 4), semana("a" to 4), semana("a" to 4), semana()))
        assertEquals(1, r.docentes.size)
        assertEquals(0, r.docentes[0].semanaPasada)
    }

    @Test
    fun `sin un habito de al menos una apertura no hay riesgo`() {
        val r = DocentesEnRiesgo.calcular("2026-11-16", listOf(semana(), semana("a" to 1), semana(), semana()))
        assertTrue(r.docentes.isEmpty())
    }

    @Test
    fun `primero van las caidas mas grandes`() {
        val r = DocentesEnRiesgo.calcular(
            "2026-11-16",
            listOf(semana("a" to 4, "b" to 10), semana("a" to 4, "b" to 10), semana("a" to 4, "b" to 10), semana("a" to 1, "b" to 2))
        )
        assertEquals(listOf("b", "a"), r.docentes.map { it.uid })
    }

    @Test
    fun `el nombre sale de la primera semana que lo trae`() {
        val semanas = listOf(
            listOf(AperturasDocente("a", null, 5)),
            listOf(AperturasDocente("a", "Ana", 5)),
            listOf(AperturasDocente("a", "Otro", 5)),
            listOf(AperturasDocente("a", null, 0))
        )
        assertEquals("Ana", DocentesEnRiesgo.calcular("2026-11-16", semanas).docentes[0].nombre)
        assertNull(DocentesEnRiesgo.calcular("2026-11-16", semanas.map { s -> s.map { it.copy(nombre = null) } }).docentes[0].nombre)
    }
}