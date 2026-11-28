package com.enad.enadmovil.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumenNivelesTest {

    private fun fila(nombre: String, guardadoEn: Long, vararg niveles: Int) =
        FilaNiveles("id-$nombre", nombre, guardadoEn, niveles.copyOf(NivelesInstitucion.NIVELES))

    @Test
    fun sumaTodasLasInstitucionesDelFiltro() {
        val resumen = ResumenNiveles(
            listOf(
                fila("A", 10L, 5, 3, 1),  // 4 evaluados, 3 en Principiante
                fila("B", 20L, 0, 1, 3)   // 4 evaluados, 1 en Principiante
            )
        )
        assertEquals(8, resumen.evaluados)
        assertEquals(4, resumen.principiantes)
        assertEquals(50.0, resumen.porcentajePrincipiante!!, 0.001)
        assertEquals("A", resumen.necesitaMasApoyo?.nombre)
        assertEquals(10L, resumen.guardadoEn)
    }

    @Test
    fun sinEvaluadosNoHayPorcentajeNiInstitucionConMasApoyo() {
        val resumen = ResumenNiveles(listOf(fila("A", 0L, 7)))
        assertNull(resumen.porcentajePrincipiante)
        assertNull(resumen.necesitaMasApoyo)
        assertNull(ResumenNiveles(emptyList()).guardadoEn)
    }

    @Test
    fun laQueQuedoConDatosViejosSeMarcaSinActualizar() {
        assertTrue(sinActualizar(fila("A", 100L), ultimoIntento = 200L))
        assertFalse(sinActualizar(fila("B", 200L), ultimoIntento = 200L))
        assertFalse(sinActualizar(fila("C", 100L), ultimoIntento = null))
    }

    @Test
    fun estadoSegunLaConexionYLaDescarga() {
        assertEquals(EstadoNiveles.VACIO, estadoSinConexion(hayFilas = false))
        assertEquals(EstadoNiveles.SIN_CONEXION, estadoSinConexion(hayFilas = true))
        assertEquals(EstadoNiveles.EN_LINEA, estadoTrasDescarga(hayFilas = true, total = 3, fallidas = 1))
        assertEquals(EstadoNiveles.SIN_CONEXION, estadoTrasDescarga(hayFilas = true, total = 3, fallidas = 3))
        assertEquals(EstadoNiveles.VACIO, estadoTrasDescarga(hayFilas = false, total = 0, fallidas = 0))
    }
}
