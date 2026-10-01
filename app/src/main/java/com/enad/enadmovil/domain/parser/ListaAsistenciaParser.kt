package com.enad.enadmovil.domain.parser

import com.enad.enadmovil.domain.model.EstudianteEscaneado
import com.enad.enadmovil.domain.model.LineaOcr
import kotlin.math.abs

/**
 * Lógica pura (sin Android): convierte los fragmentos del OCR en estudiantes.
 * Formato esperado por fila: nombre · código · grado · edad (código y demás pueden faltar).
 */
object ListaAsistenciaParser {

    private val GRADOS = 3..5
    private val EDADES = 5..99
    private val GRADOS_EN_LETRAS = mapOf("tercero" to 3, "tercer" to 3, "cuarto" to 4, "quinto" to 5)
    private val ENCABEZADOS = setOf(
        "nombre", "código", "codigo", "grado", "edad", "asistencia",
        "lista", "fecha", "colegio", "docente", "curso"
    )

    fun parsear(lineas: List<LineaOcr>): List<EstudianteEscaneado> =
        agruparEnFilas(lineas).mapNotNull { parsearFila(it) }

    /** ML Kit devuelve fragmentos sueltos: se agrupan por altura y se ordenan de izquierda a derecha. */
    private fun agruparEnFilas(lineas: List<LineaOcr>): List<String> {
        if (lineas.isEmpty()) return emptyList()
        val altoProm = lineas.map { it.bottom - it.top }.average()
        val filas = mutableListOf<MutableList<LineaOcr>>()
        var centroFila = 0.0

        lineas.sortedBy { it.top + it.bottom }.forEach { l ->
            val centro = (l.top + l.bottom) / 2.0
            if (filas.isEmpty() || abs(centro - centroFila) > altoProm * 0.6) {
                filas += mutableListOf(l)
                centroFila = centro
            } else {
                filas.last() += l
            }
        }
        return filas.map { fila -> fila.sortedBy { it.left }.joinToString(" ") { it.texto } }
    }

    private fun parsearFila(texto: String): EstudianteEscaneado? {
        val tokens = texto.split(Regex("\\s+"))
            .map { it.trim('|', ';', ',', ':') }
            .filter { it.isNotEmpty() }
            .toMutableList()
        if (tokens.isEmpty()) return null

        // Numeración de la fila ("1 Laura ..."): se descarta.
        if (tokens.size > 1 && tokens[0].length <= 2 && tokens[0].all(Char::isDigit) &&
            tokens[1].any(Char::isLetter)
        ) tokens.removeAt(0)

        val primerNumero = tokens.indexOfFirst { t -> t.any(Char::isDigit) }
        if (primerNumero < 1) return null // sin datos o sin nombre: encabezados, títulos, basura

        var grado: Int? = null
        val nombreTokens = tokens.take(primerNumero).filter { t ->
            val enLetras = GRADOS_EN_LETRAS[t.lowercase()]
            if (enLetras != null) { grado = enLetras; false } else true
        }
        if (nombreTokens.any { it.lowercase() in ENCABEZADOS }) return null
        val nombre = nombreTokens.joinToString(" ")
        if (nombre.length < 3) return null

        var codigo: String? = null
        var edad: Int? = null
        tokens.drop(primerNumero).forEach { t ->
            val digitos = t.filter(Char::isDigit)
            val valor = digitos.toIntOrNull()
            when {
                codigo == null && t.length >= 3 && digitos.isNotEmpty() -> codigo = t
                valor != null && grado == null && valor in GRADOS -> grado = valor
                valor != null && edad == null && valor in EDADES -> edad = valor
                else -> Unit
            }
        }
        return EstudianteEscaneado(nombre = nombre, codigo = codigo, grado = grado, edad = edad)
    }
}