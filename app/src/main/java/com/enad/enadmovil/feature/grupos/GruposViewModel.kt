package com.enad.enadmovil.feature.grupos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.repository.GruposRepository
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.Nino
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GruposViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = GruposRepository(application)
    private val selectedTabIndex = MutableStateFlow(0)

    init {
        viewModelScope.launch { repo.sembrarSiVacio() }
    }

    private fun areaDe(tabIndex: Int) = if (tabIndex == 0) AreaMateria.MATEMATICAS else AreaMateria.LECTURA

    val uiState: StateFlow<GruposUiState> = selectedTabIndex
        .flatMapLatest { tabIndex ->
            repo.observarGrupos(areaDe(tabIndex)).map { grupos ->
                GruposUiState(
                    subjectAreas = listOf("Matemáticas", "Lectura"),
                    selectedTabIndex = tabIndex,
                    grupos = grupos,
                    ninosSinGrupo = 3,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GruposUiState(
                subjectAreas = listOf("Matemáticas", "Lectura"),
                selectedTabIndex = 0,
                grupos = emptyList(),
                ninosSinGrupo = 0,
                isLoading = true
            )
        )

    fun onTabSelected(index: Int) {
        selectedTabIndex.value = index
    }

    fun agregarGrupo(nombre: String, ninos: List<Nino>, docente: String, cantidadNinos: Int, cantidadDocentes: Int) {
        viewModelScope.launch {
            repo.crearGrupo(nombre, docente, areaDe(selectedTabIndex.value), ninos, cantidadNinos, cantidadDocentes)
        }
    }

    fun actualizarNombre(id: Int, nuevoNombre: String) {
        viewModelScope.launch { repo.actualizarNombre(id, nuevoNombre) }
    }

    fun actualizarDocente(id: Int, nuevoDocente: String) {
        viewModelScope.launch { repo.actualizarDocente(id, nuevoDocente) }
    }

    fun quitarNino(id: Int, nino: Nino) {
        viewModelScope.launch { repo.quitarNino(id, nino) }
    }

    fun agregarNinos(id: Int, nuevos: List<Nino>) {
        viewModelScope.launch { repo.agregarNinos(id, nuevos) }
    }

    fun eliminarGrupo(id: Int) {
        viewModelScope.launch { repo.eliminarGrupo(id) }
    }
}
