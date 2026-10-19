package com.enad.enadmovil.feature.grupos

import com.enad.enadmovil.domain.model.Grupo
import com.enad.enadmovil.domain.model.Nino

data class GruposUiState(
    val subjectAreas: List<String>,
    val selectedTabIndex: Int,
    val grupos: List<Grupo>,
    val ninosSinGrupo: Int,
    val isLoading: Boolean,
    /** Estudiantes que todavía no están en ningún grupo de la materia elegida. */
    val sinGrupo: List<Nino> = emptyList()
)
