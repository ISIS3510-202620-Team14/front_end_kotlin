package com.enad.enadmovil.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.CloudFunctionsApi.Institucion
import com.enad.enadmovil.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClasificacionUiState(
    val cargando: Boolean = true,
    val mensaje: String? = null,
    val instituciones: List<Institucion> = emptyList(),
    val institucionId: String? = null,
    val grado: Int? = null,   // null = "Todos"
    val estudiantes: List<EstudianteClasificacion> = emptyList()   // todos los de la institución elegida
) {
    val visibles: List<EstudianteClasificacion>
        get() = if (grado == null) estudiantes else estudiantes.filter { it.grado == grado }
}

/**
 * Carga las instituciones del docente con sesión abierta (GET /schools) y los estudiantes
 * de la que elija (GET /students?schoolId=). El filtro por curso se hace en memoria para
 * que cambiar de chip no vuelva a pedir la lista.
 */
class ClasificacionViewModel : ViewModel() {

    private val auth = AuthRepository()
    private var materiaClave = ""
    private var cargaEstudiantes: Job? = null

    // Lo que el docente editó en la tarjeta (sexo, edad, nivel). Sobrevive a los cambios de filtro.
    private val ediciones = mutableMapOf<String, EstudianteClasificacion>()

    private val _uiState = MutableStateFlow(ClasificacionUiState())
    val uiState: StateFlow<ClasificacionUiState> = _uiState.asStateFlow()

    fun iniciar(materia: MateriaConfig) {
        if (materiaClave.isNotEmpty()) return
        materiaClave = materia.clave
        cargarInstituciones()
    }

    fun reintentar() {
        val actual = _uiState.value.institucionId
        if (actual == null) cargarInstituciones() else cargarEstudiantes(actual)
    }

    fun seleccionarInstitucion(id: String) {
        if (id == _uiState.value.institucionId) return
        _uiState.update { it.copy(institucionId = id, estudiantes = emptyList()) }
        cargarEstudiantes(id)
    }

    fun seleccionarGrado(grado: Int?) {
        _uiState.update { it.copy(grado = grado) }
    }

    fun actualizar(id: String, cambio: (EstudianteClasificacion) -> EstudianteClasificacion) {
        _uiState.update { estado ->
            estado.copy(estudiantes = estado.estudiantes.map { e ->
                if (e.id == id) cambio(e).also { ediciones[id] = it } else e
            })
        }
    }

    private fun cargarInstituciones() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensaje = null) }
            try {
                val instituciones = CloudFunctionsApi.misInstituciones(token())
                val primera = instituciones.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        instituciones = instituciones,
                        institucionId = primera,
                        cargando = primera != null,
                        mensaje = if (primera == null) "No tienes instituciones asignadas." else null
                    )
                }
                if (primera != null) cargarEstudiantes(primera)
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, mensaje = mensajeDe(e)) }
            }
        }
    }

    private fun cargarEstudiantes(schoolId: String) {
        cargaEstudiantes?.cancel()
        cargaEstudiantes = viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensaje = null) }
            try {
                val remotos = CloudFunctionsApi.listarEstudiantes(token(), schoolId, null)
                val estudiantes = remotos.mapIndexed { i, r ->
                    ediciones[r.id] ?: EstudianteClasificacion(
                        id = r.id,
                        numero = i + 1,
                        nombre = r.fullName,
                        grado = r.grade,
                        sexo = r.gender ?: "F",
                        edad = r.age?.toString().orEmpty(),
                        nivelActual = r.niveles[materiaClave]
                    )
                }
                _uiState.update { it.copy(cargando = false, estudiantes = estudiantes) }
            } catch (e: CancellationException) {
                throw e   // el docente cambió de institución antes de que llegara la lista
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, mensaje = mensajeDe(e)) }
            }
        }
    }

    private suspend fun token(): String =
        auth.obtenerToken() ?: throw CloudFunctionsApi.ApiException("unauthenticated", "Tu sesión expiró. Vuelve a iniciar sesión.", 401)

    private fun mensajeDe(e: Exception): String = when ((e as? CloudFunctionsApi.ApiException)?.code) {
        "no-school" -> "No tienes instituciones asignadas."
        "unauthenticated" -> "Tu sesión expiró. Vuelve a iniciar sesión."
        "permission-denied" -> "No tienes acceso a esta institución."
        null -> "No pudimos cargar los estudiantes. Revisa tu conexión."
        else -> e.message ?: "No pudimos cargar los estudiantes."
    }
}
