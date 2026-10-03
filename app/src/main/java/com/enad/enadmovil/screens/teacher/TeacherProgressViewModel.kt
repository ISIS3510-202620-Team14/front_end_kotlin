package com.enad.enadmovil.ui.screens.teacher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.repository.ClasificacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MateriaProgreso(
    val titulo: String,
    val evaluados: Int,
    val total: Int
) {
    val porcentaje: Int
        get() = if (total == 0) {
            0
        } else {
            (evaluados * 100) / total
        }
}

data class TeacherProgressUiState(
    val materias: List<MateriaProgreso> = emptyList(),
    val cargando: Boolean = false,
    val cargado: Boolean = false
)

class TeacherProgressViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        ClasificacionRepository(application)

    private val _uiState =
        MutableStateFlow(TeacherProgressUiState())

    val uiState: StateFlow<TeacherProgressUiState> =
        _uiState.asStateFlow()

    fun refrescar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                cargando = true
            )

            runCatching {
                repository.todosLosEstudiantes()
            }.onSuccess { estudiantes ->

                val lecturaEvaluados =
                    estudiantes.count {
                        it.niveles["lectura"] != null
                    }

                val matematicasEvaluados =
                    estudiantes.count {
                        it.niveles["matematicas"] != null
                    }

                _uiState.value = TeacherProgressUiState(
                    materias = listOf(
                        MateriaProgreso(
                            titulo = "Lectura",
                            evaluados = lecturaEvaluados,
                            total = estudiantes.size
                        ),
                        MateriaProgreso(
                            titulo = "Matemáticas",
                            evaluados = matematicasEvaluados,
                            total = estudiantes.size
                        )
                    ),
                    cargando = false,
                    cargado = true
                )

            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    cargando = false
                )
            }
        }
    }
}