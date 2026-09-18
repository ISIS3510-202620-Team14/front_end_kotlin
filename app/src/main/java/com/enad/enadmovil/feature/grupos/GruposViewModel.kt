package com.enad.enadmovil.feature.grupos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.local.EnadDatabase
import com.enad.enadmovil.data.local.GrupoEntity
import com.enad.enadmovil.data.local.GrupoNinoCrossRef
import com.enad.enadmovil.data.local.NinoEntity
import com.enad.enadmovil.data.mapper.aGrupo
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.Nino
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GruposViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = EnadDatabase.obtener(application).gruposDao()
    private val selectedTabIndex = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            if (dao.contarGrupos() == 0) {
                sembrarDatosDeEjemplo()
            }
        }
    }

    private suspend fun sembrarDatosDeEjemplo() {
        dao.insertarNinos(
            ninosDeEjemplo.map { NinoEntity(it.id, it.nombre, it.nivel, it.grado) }
        )

        val idAbejitas = dao.insertarGrupo(
            GrupoEntity(nombre = "Grupo Abejitas", docente = "Yo", area = AreaMateria.MATEMATICAS)
        ).toInt()
        dao.insertarCrossRefs(
            listOf(
                GrupoNinoCrossRef(idAbejitas, 2),
                GrupoNinoCrossRef(idAbejitas, 6)
            )
        )

        val idColibries = dao.insertarGrupo(
            GrupoEntity(nombre = "Grupo Colibríes", docente = "Prof. Nelson", area = AreaMateria.MATEMATICAS)
        ).toInt()
        dao.insertarCrossRefs(
            listOf(
                GrupoNinoCrossRef(idColibries, 1),
                GrupoNinoCrossRef(idColibries, 7)
            )
        )

        val idTortugas = dao.insertarGrupo(
            GrupoEntity(nombre = "Grupo Tortugas", docente = "Prof. Marina", area = AreaMateria.MATEMATICAS)
        ).toInt()
        dao.insertarCrossRefs(
            listOf(
                GrupoNinoCrossRef(idTortugas, 5)
            )
        )
    }

    val uiState: StateFlow<GruposUiState> = combine(
        dao.observarGrupos(),
        selectedTabIndex
    ) { grupos, tabIndex ->
        val area = if (tabIndex == 0) AreaMateria.MATEMATICAS else AreaMateria.LECTURA
        GruposUiState(
            subjectAreas = listOf("Matemáticas", "Lectura"),
            selectedTabIndex = tabIndex,
            grupos = grupos.filter { it.grupo.area == area }.map { it.aGrupo() },
            ninosSinGrupo = 3,
            isLoading = false
        )
    }.stateIn(
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

    fun agregarGrupo(nombre: String, ninos: List<Nino>, docente: String) {
        viewModelScope.launch {
            val area = if (selectedTabIndex.value == 0) AreaMateria.MATEMATICAS else AreaMateria.LECTURA
            val nuevoId = dao.insertarGrupo(
                GrupoEntity(nombre = nombre, docente = docente, area = area)
            ).toInt()
            dao.insertarCrossRefs(ninos.map { GrupoNinoCrossRef(nuevoId, it.id) })
        }
    }

    fun actualizarNombre(id: Int, nuevoNombre: String) {
        viewModelScope.launch { dao.actualizarNombre(id, nuevoNombre) }
    }

    fun actualizarDocente(id: Int, nuevoDocente: String) {
        viewModelScope.launch { dao.actualizarDocente(id, nuevoDocente) }
    }

    fun quitarNino(id: Int, nino: Nino) {
        viewModelScope.launch { dao.quitarNinoDeGrupo(id, nino.id) }
    }

    fun agregarNinos(id: Int, nuevos: List<Nino>) {
        viewModelScope.launch { dao.insertarCrossRefs(nuevos.map { GrupoNinoCrossRef(id, it.id) }) }
    }

    fun eliminarGrupo(id: Int) {
        viewModelScope.launch { dao.eliminarGrupo(id) }
    }
}
