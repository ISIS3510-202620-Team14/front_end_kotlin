package com.enad.enadmovil.domain.parser

import com.enad.enadmovil.domain.model.EstudianteEscaneado
import java.text.Normalizer

/**
 * Lógica pura (sin Android): junta las filas de una página nueva con las que ya se leyeron.
 * Si el docente fotografía dos veces la misma hoja, o dos fotos se solapan, el mismo
 * estudiante no queda repetido.
 */
object FusionFilas {

    /** Filas de [leidas] que todavía no están en [existentes], sin repetir entre ellas. */
    fun nuevas(existentes: List<EstudianteEscaneado>, leidas: List<EstudianteEscaneado>): List<EstudianteEscaneado> {
        val vistas = existentes.mapTo(mutableSetOf()) { clave(it) }
        return leidas.filter { vistas.add(clave(it)) }
    }

    // Con código, el código manda. Sin código se compara el nombre sin tildes, mayúsculas ni espacios de más.
    private fun clave(fila: EstudianteEscaneado): String =
        fila.codigo?.trim()?.takeIf { it.isNotEmpty() }?.let { "codigo:$it" }
            ?: "nombre:${normalizar(fila.nombre)}"

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("\\s+"), " ")
}
