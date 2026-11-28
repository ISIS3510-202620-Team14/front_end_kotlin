package com.enad.enadmovil.data.niveles

import com.enad.enadmovil.domain.model.NivelesInstitucion
import org.junit.Assert.assertEquals
import org.junit.Test

class ConteoEstudiantesTest {

    private fun celda(materia: Int, grado: Int, nivel: Int) = NivelesInstitucion.indice(materia, grado, nivel)

    @Test
    fun cuentaCadaMateriaEnSuGradoYNivel() {
        val conteos = contarEstudiantes(listOf(
            EstudianteNivel(3, false, mapOf("lectura" to 1, "matematicas" to 4)),
            EstudianteNivel(3, false, mapOf("lectura" to 1, "matematicas" to null)),
            EstudianteNivel(5, false, mapOf("lectura" to 6))
        ))
        assertEquals(2, conteos[celda(0, 0, 1)]) // lectura, grado 3, Principiante
        assertEquals(1, conteos[celda(1, 0, 4)]) // matemáticas, grado 3, nivel 4
        assertEquals(1, conteos[celda(1, 0, 0)]) // matemáticas sin evaluar
        assertEquals(1, conteos[celda(0, 2, 6)]) // lectura, grado 5, Comprensión
        assertEquals(1, conteos[celda(1, 2, 0)]) // sin la materia = sin evaluar
        assertEquals(6, conteos.sum())           // 3 estudiantes x 2 materias
    }

    @Test
    fun saltaRetiradosYGradosFueraDeRango() {
        val conteos = contarEstudiantes(listOf(
            EstudianteNivel(4, true, mapOf("lectura" to 2)),
            EstudianteNivel(2, false, mapOf("lectura" to 2)),
            EstudianteNivel(0, false, emptyMap())
        ))
        assertEquals(0, conteos.sum())
    }

    @Test
    fun unNivelRaroCuentaComoSinEvaluar() {
        val conteos = contarEstudiantes(listOf(EstudianteNivel(4, false, mapOf("lectura" to 9, "matematicas" to -1))))
        assertEquals(1, conteos[celda(0, 1, 0)])
        assertEquals(1, conteos[celda(1, 1, 0)])
    }
}
