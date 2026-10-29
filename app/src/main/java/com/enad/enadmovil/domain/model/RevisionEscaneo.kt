package com.enad.enadmovil.domain.model

/** Una fila del escaneo: lo que leyó el OCR y cómo la dejó el docente en la revisión. */
data class FilaRevisada(val leida: EstudianteEscaneado, val actual: EstudianteEscaneado = leida)

/**
 * BQ #11: qué tanto corrigió el docente lo que leyó el escáner antes de importar.
 * Solo son conteos: nunca viaja un nombre ni un código.
 */
data class ResumenRevision(
    val leidas: Int,           // filas que leyó el OCR (las borradas incluidas)
    val sinCambios: Int,
    val editadas: Int,         // con al menos un campo corregido
    val nombresEditados: Int,
    val codigosEditados: Int,
    val borradas: Int
) {
    companion object {
        /** [conservadas]: las filas que siguen en la lista al importar. [borradas]: las que el docente quitó. */
        fun de(conservadas: List<FilaRevisada>, borradas: Int): ResumenRevision {
            val nombre = conservadas.count { normalizar(it.leida.nombre) != normalizar(it.actual.nombre) }
            val codigo = conservadas.count { normalizar(it.leida.codigo) != normalizar(it.actual.codigo) }
            val editadas = conservadas.count {
                normalizar(it.leida.nombre) != normalizar(it.actual.nombre) ||
                    normalizar(it.leida.codigo) != normalizar(it.actual.codigo)
            }
            return ResumenRevision(
                leidas = conservadas.size + borradas,
                sinCambios = conservadas.size - editadas,
                editadas = editadas,
                nombresEditados = nombre,
                codigosEditados = codigo,
                borradas = borradas
            )
        }

        // Se compara como se importa: sin espacios de sobra y un código vacío es "sin código"
        private fun normalizar(texto: String?): String? = texto?.trim()?.ifBlank { null }
    }
}
