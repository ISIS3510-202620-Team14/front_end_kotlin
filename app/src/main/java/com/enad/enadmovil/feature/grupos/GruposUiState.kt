package com.enad.enadmovil.feature.grupos

import com.enad.enadmovil.domain.model.Grupo

data class GruposUiState(val subjectAreas: List<String>, val selectedTabIndex: Int,val grupos: List<Grupo>, val ninosSinGrupo: Int,val isLoading: Boolean )