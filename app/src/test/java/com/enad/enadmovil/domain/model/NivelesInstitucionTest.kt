package com.enad.enadmovil.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NivelesInstitucionTest {

    private fun fila(nombre: String, vararg niveles: Int) =
        FilaNiveles("id-$nombre", nombre, 0L, niveles.copyOf(NivelesInstitucion.NIVELES))

    @Test
    fun cadaCeldaTieneSuPropiaPosicion() {
        assertEquals(42, NivelesInstitucion.TAMANO)
        assertEquals(0, NivelesInstitucion.indice(0, 0, 0))
        assertEquals(7, NivelesInstitucion.indice(0, 1, 0)) // lectura, grado 4
        assertEquals(21, NivelesInstitucion.indice(1, 0, 0)) // matemáticas, grado 3
        assertEquals(41, NivelesInstitucion.indice(1, 2, 6))
    }

    @Test
    fun losSinEvaluarNoCuentanEnElPorcentaje() {
        val f = fila("A", 10, 3, 2, 1, 0, 0, 0) // 10 sin evaluar, 6 evaluados, 3 en Principiante
        assertEquals(10, f.pendientes)
        assertEquals(6, f.evaluados)
        assertEquals(50.0, f.porcentajePrincipiante!!, 0.001)
    }

    @Test
    fun sinEvaluadosNoHayPorcentaje() {
        assertNull(fila("A", 5).porcentajePrincipiante)
    }

    @Test
    fun primeroLaQueMasApoyoNecesita() {
        val ordenadas = listOf(
            fila("Sin datos", 4),
            fila("Baja", 0, 1, 3),   // 25 %
            fila("Alta", 0, 3, 1),   // 75 %
            fila("Alta B", 0, 3, 1)  // 75 %, va después por nombre
        ).sortedWith(FilaNiveles.porApoyoNecesario)
        assertEquals(listOf("Alta", "Alta B", "Baja", "Sin datos"), ordenadas.map { it.nombre })
    }
}
