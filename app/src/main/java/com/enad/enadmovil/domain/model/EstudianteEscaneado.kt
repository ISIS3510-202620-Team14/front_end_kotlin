package com.enad.enadmovil.domain.model

/** Una fila de la lista física ya interpretada. null = el OCR no lo encontró. */
data class EstudianteEscaneado(
    val nombre: String,
    val codigo: String? = null,
    val grado: Int? = null,   // el backend solo acepta 3, 4 o 5
    val edad: Int? = null,
    val presente: Boolean? = null
)