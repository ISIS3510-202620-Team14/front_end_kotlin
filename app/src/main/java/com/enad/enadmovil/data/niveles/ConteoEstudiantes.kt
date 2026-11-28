package com.enad.enadmovil.data.niveles

import com.enad.enadmovil.domain.model.NivelesInstitucion

/** Lo que hace falta de cada estudiante para contarlo: grado, si está retirado y su nivel por materia. */
data class EstudianteNivel(
    val grado: Int,
    val retirado: Boolean,
    /** Materia -> orden del nivel actual (1..6); null o ausente = todavía sin evaluar. */
    val ordenPorMateria: Map<String, Int?>
)

/**
 * Cuenta los estudiantes de una institución en los 42 conteos de NivelesInstitucion (materia x grado
 * x nivel, 0 = sin evaluar). Salta a los retirados y a los grados fuera de 3, 4 y 5.
 */
fun contarEstudiantes(estudiantes: List<EstudianteNivel>): IntArray {
    val conteos = IntArray(NivelesInstitucion.TAMANO)
    for (i in estudiantes.indices) {
        val estudiante = estudiantes[i]
        if (estudiante.retirado) continue
        val grado = NivelesInstitucion.GRADOS.indexOf(estudiante.grado)
        if (grado < 0) continue
        for (m in NivelesInstitucion.MATERIAS.indices) {
            var orden = estudiante.ordenPorMateria[NivelesInstitucion.MATERIAS[m]] ?: 0
            if (orden !in 0 until NivelesInstitucion.NIVELES) orden = 0
            conteos[NivelesInstitucion.indice(m, grado, orden)]++
        }
    }
    return conteos
}
