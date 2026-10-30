package com.enad.enadmovil.domain.parser

import com.enad.enadmovil.domain.model.EstudianteEscaneado
import java.text.Normalizer

/** Un estudiante que ya está guardado en el colegio, con lo necesario para compararlo. */
data class EstudianteConocido(val id: String, val nombre: String, val codigo: String?, val grado: Int?)

/** Qué tiene que ver una fila escaneada con los estudiantes que ya existen. */
sealed interface Coincidencia {
    val con: EstudianteConocido

    /** Es el mismo estudiante: al importar no se crea otra vez. */
    data class Igual(override val con: EstudianteConocido) : Coincidencia

    /** Puede ser el mismo con un error al escribir o al leer. Decide el docente. */
    data class Parecido(override val con: EstudianteConocido) : Coincidencia
}

/**
 * Lógica pura (sin Android): busca si una fila del escaneo ya existe en el colegio.
 * Los docentes a veces escriben el nombre sin tildes, con los apellidos primero o con una
 * letra cambiada, y el OCR también se equivoca. Sin esto cada error creaba un estudiante repetido.
 */
object CoincidenciaEstudiante {

    fun buscar(fila: EstudianteEscaneado, conocidos: List<EstudianteConocido>, gradoPorDefecto: Int?): Coincidencia? {
        val codigo = fila.codigo?.trim()?.ifBlank { null }
        val grado = fila.grado ?: gradoPorDefecto
        val nombre = normalizar(fila.nombre)

        // Con código, el código manda
        if (codigo != null) {
            conocidos.firstOrNull { it.codigo?.trim().equals(codigo, ignoreCase = true) }?.let { return Coincidencia.Igual(it) }
        }
        // Mismo nombre y mismo grado, y la fila no trae un código distinto al guardado
        conocidos.firstOrNull {
            normalizar(it.nombre) == nombre && (grado == null || it.grado == grado) &&
                (codigo == null || it.codigo.isNullOrBlank())
        }?.let { return if (codigo == null) Coincidencia.Igual(it) else Coincidencia.Parecido(it) }

        return conocidos.firstOrNull { seParecen(nombre, normalizar(it.nombre)) }?.let { Coincidencia.Parecido(it) }
    }

    /** Mismo nombre con otro orden, o una o dos letras distintas según el largo. */
    internal fun seParecen(a: String, b: String): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b || ordenado(a) == ordenado(b)) return true
        val permitidas = if (minOf(a.length, b.length) < 10) 1 else 2
        return distancia(a, b) <= permitidas
    }

    /** Sin tildes, mayúsculas ni espacios de más. */
    internal fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("\\s+"), " ")

    private fun ordenado(nombre: String) = nombre.split(" ").sorted().joinToString(" ")

    // Distancia de Levenshtein: cuántas letras hay que cambiar, quitar o poner
    private fun distancia(a: String, b: String): Int {
        var previa = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val actual = IntArray(b.length + 1)
            actual[0] = i
            for (j in 1..b.length) {
                val costo = if (a[i - 1] == b[j - 1]) 0 else 1
                actual[j] = minOf(previa[j] + 1, actual[j - 1] + 1, previa[j - 1] + costo)
            }
            previa = actual
        }
        return previa[b.length]
    }
}
