package com.enad.enadmovil.ui.screens.teacher.scan

import com.enad.enadmovil.domain.model.EstudianteEscaneado

data class ScanUiState(
    val procesando: Boolean = false,
    val filas: List<EstudianteEscaneado> = emptyList(),
    val error: String? = null
)