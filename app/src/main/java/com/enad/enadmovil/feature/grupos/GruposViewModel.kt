package com.enad.enadmovil.feature.grupos

import androidx.lifecycle.ViewModel
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.Grupo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GruposViewModel: ViewModel() {
    private var todosLosGrupos = listOf(Grupo("Grupo Abejitas", 2, "Yo", AreaMateria.MATEMATICAS),
                Grupo("Grupo Colibries", 2, "Porf. Nelson", AreaMateria.MATEMATICAS),
                Grupo("Grupo Tortugas", 1, "Prof. Marina", AreaMateria.MATEMATICAS))
    private val _uiState = MutableStateFlow(GruposUiState(subjectAreas = listOf("Matemáticas", "Lectura"), selectedTabIndex = 0, grupos = todosLosGrupos.filter { it.area == AreaMateria.MATEMATICAS }, ninosSinGrupo = 3, isLoading = false))
    val uiState: StateFlow<GruposUiState> = _uiState.asStateFlow()

    fun onTabSelected(index: Int) {
        val area = if (index == 0) AreaMateria.MATEMATICAS else AreaMateria.LECTURA
        _uiState.update { currentState ->
            currentState.copy(selectedTabIndex = index, grupos = todosLosGrupos.filter { it.area == area})
        }
    }

    fun agregarGrupo(nombre: String, cantidadNinos: Int, docente: String) {
        val area = if (_uiState.value.selectedTabIndex == 0) AreaMateria.MATEMATICAS else AreaMateria.LECTURA
        todosLosGrupos = todosLosGrupos + Grupo(nombre, cantidadNinos, docente, area)
        _uiState.update { currentState ->
            currentState.copy(grupos = todosLosGrupos.filter { it.area == area })
        }
    }
}