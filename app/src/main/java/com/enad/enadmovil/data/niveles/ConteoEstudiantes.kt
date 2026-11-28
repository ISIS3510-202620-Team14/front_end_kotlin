package com.enad.enadmovil.data.niveles

import com.enad.enadmovil.domain.model.NivelesInstitucion

private const val PRIMER_GRADO = 3 // NivelesInstitucion.GRADOS = 3, 4 y 5

/**
 * Suma un estudiante a los 42 conteos de NivelesInstitucion (materia x grado x nivel, 0 = sin evaluar).
 * Salta a los retirados y a los grados fuera de 3, 4 y 5; un orden fuera de 1..6 cuenta como sin evaluar.
 *
 * MICRO-OPTIMIZACIÓN (Sprint 4): recibe primitivos, así que no se crea ningún objeto por estudiante
 * (antes: un EstudianteNivel, un HashMap y una lista), y el grado se ubica con una resta en vez de
 * List.indexOf, que compara Integers.
 */
fun sumarEstudiante(conteos: IntArray, grado: Int, retirado: Boolean, ordenLectura: Int, ordenMatematicas: Int) {
    if (retirado) return
    val g = grado - PRIMER_GRADO
    if (g < 0 || g >= NivelesInstitucion.GRADOS.size) return
    conteos[NivelesInstitucion.indice(0, g, nivelValido(ordenLectura))]++
    conteos[NivelesInstitucion.indice(1, g, nivelValido(ordenMatematicas))]++
}

private fun nivelValido(orden: Int): Int = if (orden in 1 until NivelesInstitucion.NIVELES) orden else 0
