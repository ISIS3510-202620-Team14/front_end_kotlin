package com.enad.enadmovil.feature.grupos

import androidx.lifecycle.ViewModel
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.Grupo
import com.enad.enadmovil.domain.model.Nino
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GruposViewModel: ViewModel() {
    private var siguienteId = 4
    private var todosLosGrupos = listOf(
        Grupo(
            id = 1,
            nombre = "Grupo Abejitas",
            ninos = listOf(
                Nino(2, "Juan Carlos Cruz", "Principiante", 3),
                Nino(6, "Sofía Betancur", "Principiante", 5)
            ),
            docente = "Yo",
            area = AreaMateria.MATEMATICAS
        ),
        Grupo(
            id = 2,
            nombre = "Grupo Colibríes",
            ninos = listOf(
                Nino(1, "María López Quintero", "1 dígito", 3),
                Nino(7, "Andrés Mejía", "Principiante", 4)
            ),
            docente = "Prof. Nelson",
            area = AreaMateria.MATEMATICAS
        ),
        Grupo(
            id = 3,
            nombre = "Grupo Tortugas",
            ninos = listOf(
                Nino(5, "Pedro Ramírez", "Sin nivel", 4)
            ),
            docente = "Prof. Marina",
            area = AreaMateria.MATEMATICAS
        )
    )
    private val _uiState = MutableStateFlow(GruposUiState(subjectAreas = listOf("Matemáticas", "Lectura"), selectedTabIndex = 0, grupos = todosLosGrupos.filter { it.area == AreaMateria.MATEMATICAS }, ninosSinGrupo = 3, isLoading = false))
    val uiState: StateFlow<GruposUiState> = _uiState.asStateFlow()

    private fun refrescar() {
        val area = if (_uiState.value.selectedTabIndex == 0) AreaMateria.MATEMATICAS else AreaMateria.LECTURA
        _uiState.update { it.copy(grupos = todosLosGrupos.filter { g -> g.area == area}) }
    }
    fun onTabSelected(index: Int) {
        val area = if (index == 0) AreaMateria.MATEMATICAS else AreaMateria.LECTURA
        _uiState.update { currentState ->
            currentState.copy(selectedTabIndex = index, grupos = todosLosGrupos.filter { it.area == area})
        }
    }

    fun agregarGrupo(nombre: String, ninos: List<Nino>, docente: String) {
        val area = if (_uiState.value.selectedTabIndex == 0) AreaMateria.MATEMATICAS else AreaMateria.LECTURA
        todosLosGrupos = todosLosGrupos + Grupo(siguienteId++, nombre, ninos, docente, area)
        refrescar()
    }

    fun actualizarNombre(id: Int, nuevoNombre: String) {
        todosLosGrupos = todosLosGrupos.map { if (it.id == id) it.copy(nombre = nuevoNombre) else it}
        refrescar()
    }

    fun actualizarDocente(id: Int, nuevoDocente: String) {
        todosLosGrupos = todosLosGrupos.map{ if (it.id == id) it.copy(docente = nuevoDocente) else it}
        refrescar()
    }

    fun quitarNino(id: Int, nino: Nino) {
        todosLosGrupos = todosLosGrupos.map { if(it.id == id) it.copy(ninos = it.ninos - nino) else it }
        refrescar()
    }

    fun agregarNinos(id: Int, nuevos: List<Nino>) {
        todosLosGrupos = todosLosGrupos.map { if(it.id == id) it.copy(ninos = it.ninos + nuevos) else it }
        refrescar()
    }

    fun eliminarGrupo(id: Int) {
        todosLosGrupos = todosLosGrupos.filter { it.id != id}
        refrescar()
    }
}