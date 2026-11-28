package com.enad.enadmovil.data.niveles

import com.enad.enadmovil.domain.model.NivelesInstitucion
import org.junit.Assert.assertEquals
import org.junit.Test

class ConteoEstudiantesTest {

    private fun celda(materia: Int, grado: Int, nivel: Int) = NivelesInstitucion.indice(materia, grado, nivel)

    @Test
    fun cuentaCadaMateriaEnSuGradoYNivel() {
        val conteos = IntArray(NivelesInstitucion.TAMANO)
        sumarEstudiante(conteos, grado = 3, retirado = false, ordenLectura = 1, ordenMatematicas = 4)
        sumarEstudiante(conteos, grado = 3, retirado = false, ordenLectura = 1, ordenMatematicas = 0)
        sumarEstudiante(conteos, grado = 5, retirado = false, ordenLectura = 6, ordenMatematicas = 0)

        assertEquals(2, conteos[celda(0, 0, 1)]) // lectura, grado 3, Principiante
        assertEquals(1, conteos[celda(1, 0, 4)]) // matemáticas, grado 3, nivel 4
        assertEquals(1, conteos[celda(1, 0, 0)]) // matemáticas sin evaluar
        assertEquals(1, conteos[celda(0, 2, 6)]) // lectura, grado 5, Comprensión
        assertEquals(1, conteos[celda(1, 2, 0)]) // sin nivel = sin evaluar
        assertEquals(6, conteos.sum())           // 3 estudiantes x 2 materias
    }

    @Test
    fun saltaRetiradosYGradosFueraDeRango() {
        val conteos = IntArray(NivelesInstitucion.TAMANO)
        sumarEstudiante(conteos, grado = 4, retirado = true, ordenLectura = 2, ordenMatematicas = 2)
        sumarEstudiante(conteos, grado = 2, retirado = false, ordenLectura = 2, ordenMatematicas = 2)
        sumarEstudiante(conteos, grado = 6, retirado = false, ordenLectura = 2, ordenMatematicas = 2)
        assertEquals(0, conteos.sum())
    }

    @Test
    fun unNivelRaroCuentaComoSinEvaluar() {
        val conteos = IntArray(NivelesInstitucion.TAMANO)
        sumarEstudiante(conteos, grado = 4, retirado = false, ordenLectura = 9, ordenMatematicas = -1)
        assertEquals(1, conteos[celda(0, 1, 0)])
        assertEquals(1, conteos[celda(1, 1, 0)])
    }
}
