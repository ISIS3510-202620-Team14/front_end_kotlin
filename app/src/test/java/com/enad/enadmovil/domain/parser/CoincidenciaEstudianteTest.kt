package com.enad.enadmovil.domain.parser

import com.enad.enadmovil.domain.model.EstudianteEscaneado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoincidenciaEstudianteTest {

    private val laura = EstudianteConocido("1", "Laura Gómez", "1001", 4)
    private val pedro = EstudianteConocido("2", "Pedro Ruiz Castañeda", null, 4)
    private val conocidos = listOf(laura, pedro)

    private fun fila(nombre: String, codigo: String? = null, grado: Int? = null) =
        EstudianteEscaneado(nombre = nombre, codigo = codigo, grado = grado)

    @Test
    fun elMismoCodigoEsElMismoEstudiante() {
        val r = CoincidenciaEstudiante.buscar(fila("Laura Gomes", "1001"), conocidos, null)
        assertEquals(Coincidencia.Igual(laura), r)
    }

    @Test
    fun sinCodigoElMismoNombreSinTildesEnElMismoGradoEsElMismo() {
        val r = CoincidenciaEstudiante.buscar(fila("pedro  ruiz castaneda"), conocidos, 4)
        assertEquals(Coincidencia.Igual(pedro), r)
    }

    @Test
    fun elMismoNombreEnOtroGradoSoloSeParece() {
        val r = CoincidenciaEstudiante.buscar(fila("Pedro Ruiz Castañeda", grado = 5), conocidos, null)
        assertEquals(Coincidencia.Parecido(pedro), r)
    }

    @Test
    fun elMismoNombreConOtroCodigoSoloSeParece() {
        val r = CoincidenciaEstudiante.buscar(fila("Laura Gómez", "2002"), conocidos, 4)
        assertEquals(Coincidencia.Parecido(laura), r)
    }

    @Test
    fun unaLetraCambiadaOLosApellidosPrimeroSeParecen() {
        assertEquals(Coincidencia.Parecido(pedro), CoincidenciaEstudiante.buscar(fila("Pedro Ruis Castañeda"), conocidos, 4))
        assertEquals(Coincidencia.Parecido(pedro), CoincidenciaEstudiante.buscar(fila("Ruiz Castañeda Pedro"), conocidos, 4))
    }

    @Test
    fun unNombreDistintoNoCoincide() {
        assertNull(CoincidenciaEstudiante.buscar(fila("Ana Torres"), conocidos, 4))
        assertNull(CoincidenciaEstudiante.buscar(fila("Laura Gil", "3003"), conocidos, 4))
    }

    @Test
    fun enNombresCortosSoloSeAceptaUnaLetraDistinta() {
        assertTrue(CoincidenciaEstudiante.seParecen("ana ruiz", "ana ruis"))
        assertTrue(!CoincidenciaEstudiante.seParecen("ana ruiz", "ana rios"))
    }
}
