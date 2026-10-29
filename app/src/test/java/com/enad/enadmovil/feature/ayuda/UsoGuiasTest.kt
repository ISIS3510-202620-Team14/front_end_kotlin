package com.enad.enadmovil.feature.ayuda

import org.junit.Assert.assertEquals
import org.junit.Test

class UsoGuiasTest {

    private fun conteos(vararg pares: Pair<Guia, Pair<Int, Int>>): Map<Guia, Pair<Int, Int>> =
        Guia.entries.associateWith { 0 to 0 } + pares.toMap()

    @Test
    fun sinUsoNoHayGuiaMasUsada() {
        val resumen = UsoGuias.resumir(conteos())

        assertEquals(0, resumen.iniciadas)
        assertEquals(0, resumen.completadas)
        assertEquals(emptyList<Guia>(), resumen.masUsadas)
    }

    @Test
    fun laMasUsadaEsLaDeMasInicios() {
        val resumen = UsoGuias.resumir(
            conteos(Guia.HORAS to (3 to 2), Guia.ASISTENCIA to (1 to 1))
        )

        assertEquals(4, resumen.iniciadas)
        assertEquals(3, resumen.completadas)
        assertEquals(listOf(Guia.HORAS), resumen.masUsadas)
    }

    @Test
    fun siDosEmpatanSalenLasDos() {
        val resumen = UsoGuias.resumir(
            conteos(Guia.EVALUACION to (2 to 0), Guia.ASISTENCIA to (2 to 1))
        )

        assertEquals(listOf(Guia.ASISTENCIA, Guia.EVALUACION), resumen.masUsadas)
    }

    @Test
    fun lasCompletadasNoPasanDeLasIniciadas() {
        val resumen = UsoGuias.resumir(conteos(Guia.ACTIVIDADES to (1 to 3)))

        assertEquals(1, resumen.iniciadas)
        assertEquals(1, resumen.completadas)
    }
}
