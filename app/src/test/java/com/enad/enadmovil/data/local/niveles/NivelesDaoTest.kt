package com.enad.enadmovil.data.local.niveles

import com.enad.enadmovil.domain.model.NivelesInstitucion
import org.junit.Assert.assertEquals
import org.junit.Test

class NivelesDaoTest {

    @Test
    fun soloSeGuardanLasCeldasConEstudiantes() {
        val conteos = IntArray(NivelesInstitucion.TAMANO)
        conteos[NivelesInstitucion.indice(0, 0, 1)] = 4 // lectura, grado 3, Principiante
        conteos[NivelesInstitucion.indice(1, 2, 0)] = 2 // matemáticas, grado 5, sin evaluar

        val filas = aConteos("uid", "s1", "Escuela", conteos, 100L)

        assertEquals(2, filas.size)
        assertEquals(ConteoNivelEntity("uid", "s1", "Escuela", "lectura", 3, 1, 4, 100L), filas[0])
        assertEquals(ConteoNivelEntity("uid", "s1", "Escuela", "matematicas", 5, 0, 2, 100L), filas[1])
    }

    @Test(expected = IllegalArgumentException::class)
    fun rechazaConteosIncompletos() {
        aConteos("uid", "s1", "Escuela", IntArray(10), 0L)
    }

    @Test
    fun agrupaUnaFilaPorInstitucionYOrdenaPorApoyo() {
        val sumas = listOf(
            SumaNivel("a", "Escuela A", 1, 1, 5L),
            SumaNivel("a", "Escuela A", 2, 3, 5L),  // A: 25 % en Principiante
            SumaNivel("b", "Escuela B", 1, 2, 7L),
            SumaNivel("b", "Escuela B", 0, 9, 7L)   // B: 100 %, los sin evaluar no cuentan
        )

        val filas = agruparPorInstitucion(sumas)

        assertEquals(listOf("b", "a"), filas.map { it.schoolId })
        assertEquals(9, filas[0].pendientes)
        assertEquals(100.0, filas[0].porcentajePrincipiante!!, 0.001)
        assertEquals(4, filas[1].evaluados)
    }
}
