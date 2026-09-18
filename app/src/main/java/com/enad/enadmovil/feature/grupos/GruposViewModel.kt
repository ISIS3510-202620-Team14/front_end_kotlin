package com.enad.enadmovil.feature.grupos

import androidx.lifecycle.ViewModel
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.Grupo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


val gruposDeEjemplo = listOf(Grupo("Grupo Abejitas", 2, "Yo", AreaMateria.MATEMATICAS), Grupo("Grupo Colibríes", 2, "Prof. Nelson" ,
    AreaMateria.MATEMATICAS),
    Grupo("Grupo Tortugas", 1, "Prof. Marina", AreaMateria.MATEMATICAS))

class GruposViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(GruposUiState(subjectAreas = listOf("Matemáticas", "Lectura"), selectedTabIndex = 0, grupos = gruposDeEjemplo.filter {it.area == AreaMateria.MATEMATICAS}, ninosSinGrupo = 3, isLoading = false))
    val uiState: StateFlow<GruposUiState> = _uiState.asStateFlow()

    fun onTabSelected(index: Int) {
        val area = if (index == 0) AreaMateria.MATEMATICAS else AreaMateria.LECTURA
        _uiState.update { currentState ->
            currentState.copy(selectedTabIndex = index, grupos = gruposDeEjemplo.filter { it.area == area})
        }
    }
}