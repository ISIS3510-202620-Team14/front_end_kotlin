package com.enad.enadmovil.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistribucionAutomaticaTest {

    private val ninos = listOf(
        Nino(1, "Ana", "Principiante", 4),
        Nino(2, "Beto", "1 dígito", 3),
        Nino(3, "Caro", "Principiante", 3),
        Nino(4, "Dani", "1 dígito", 4)
    )

    @Test
    fun juntaALosNinosDelMismoNivel() {
        val grupo = armarGrupoAutomatico(ninos, 2)
        assertEquals(listOf(2, 4), grupo.map { it.id })
    }

    @Test
    fun noPasaDeLaCantidadPedida() {
        assertEquals(3, armarGrupoAutomatico(ninos, 3).size)
        assertEquals(4, armarGrupoAutomatico(ninos, 10).size)
    }

    @Test
    fun conCantidadCeroONegativaNoDevuelveNinos() {
        assertTrue(armarGrupoAutomatico(ninos, 0).isEmpty())
        assertTrue(armarGrupoAutomatico(ninos, -2).isEmpty())
    }
}
