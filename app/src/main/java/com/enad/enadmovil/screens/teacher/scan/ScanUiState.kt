package com.enad.enadmovil.ui.screens.teacher.scan

import com.enad.enadmovil.domain.model.EstudianteEscaneado

data class ScanUiState(
    val procesando: Boolean = false,
    val filas: List<EstudianteEscaneado> = emptyList(),
    val error: String? = null,
    val paginas: List<PaginaEscaneada> = emptyList()
)

/** Una foto tomada en el escáner y en qué va su lectura. */
data class PaginaEscaneada(val numero: Int, val estado: EstadoPagina)

sealed interface EstadoPagina {
    data object EnCola : EstadoPagina
    data object Leyendo : EstadoPagina
    data class Lista(val filas: Int) : EstadoPagina
    data object Fallo : EstadoPagina
}
