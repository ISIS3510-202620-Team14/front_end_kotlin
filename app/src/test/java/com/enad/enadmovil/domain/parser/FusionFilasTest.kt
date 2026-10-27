package com.enad.enadmovil.domain.parser

import com.enad.enadmovil.domain.model.EstudianteEscaneado
import org.junit.Assert.assertEquals
import org.junit.Test

class FusionFilasTest {

    private fun fila(nombre: String, codigo: String? = null) = EstudianteEscaneado(nombre = nombre, codigo = codigo)

    @Test
    fun sinFilasPreviasEntranTodas() {
        val leidas = listOf(fila("Laura Gómez", "1001"), fila("Pedro Ruiz"))
        assertEquals(leidas, FusionFilas.nuevas(emptyList(), leidas))
    }

    @Test
    fun elMismoCodigoNoSeRepiteAunqueElNombreCambie() {
        val existentes = listOf(fila("Laura Gómez", "1001"))
        val leidas = listOf(fila("Laura Gomes", "1001"), fila("Pedro Ruiz", "1002"))
        assertEquals(listOf(fila("Pedro Ruiz", "1002")), FusionFilas.nuevas(existentes, leidas))
    }

    @Test
    fun sinCodigoComparaElNombreSinTildesNiMayusculas() {
        val existentes = listOf(fila("María  José Peña"))
        val leidas = listOf(fila("maria jose pena"), fila("Ana Torres"))
        assertEquals(listOf(fila("Ana Torres")), FusionFilas.nuevas(existentes, leidas))
    }

    @Test
    fun noRepiteEstudiantesDentroDeLaMismaPagina() {
        val leidas = listOf(fila("Ana Torres"), fila("ANA TORRES"), fila("Luis Mora", "2001"), fila("Luis M.", "2001"))
        assertEquals(listOf(fila("Ana Torres"), fila("Luis Mora", "2001")), FusionFilas.nuevas(emptyList(), leidas))
    }

    @Test
    fun conservaElOrdenDeLasFilasLeidas() {
        val leidas = listOf(fila("Carla"), fila("Bruno"), fila("Ana"))
        assertEquals(listOf("Carla", "Bruno", "Ana"), FusionFilas.nuevas(emptyList(), leidas).map { it.nombre })
    }
}
