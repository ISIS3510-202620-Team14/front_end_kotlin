package com.enad.enadmovil.data.niveles

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContarNivelesTest {

    @Test
    fun cuentaFueraDelHiloQueLaLlama() = runBlocking {
        val llamador = Thread.currentThread().name
        var hiloDelConteo = ""
        val conteos = contarEnSegundoPlano("{}") {
            hiloDelConteo = Thread.currentThread().name
            intArrayOf(1, 2, 3)
        }
        assertArrayEquals(intArrayOf(1, 2, 3), conteos)
        assertNotEquals(llamador, hiloDelConteo)
        assertTrue(hiloDelConteo.startsWith("DefaultDispatcher"))
    }

    @Test
    fun variasInstitucionesSeCuentanCadaUnaConSuResultado() = runBlocking {
        val instituciones = listOf("a", "bb", "ccc")
        val resultados = instituciones.map { json ->
            async { contarEnSegundoPlano(json) { intArrayOf(it.length) } }
        }.awaitAll()
        assertEquals(listOf(1, 2, 3), resultados.map { it[0] })
    }
}
