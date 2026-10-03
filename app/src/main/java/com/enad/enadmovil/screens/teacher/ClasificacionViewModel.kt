package com.enad.enadmovil.ui.screens.teacher

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.CloudFunctionsApi.Institucion
import com.enad.enadmovil.data.repository.ClasificacionRepository
import com.enad.enadmovil.data.telemetria.InfoDispositivo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

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

/** Nivel especial que saca al estudiante del programa: no cuenta como clasificación para la BQ 9. */
internal const val NIVEL_RETIRADO = "Retirado"

/**
 * Carga las instituciones del docente con sesión abierta (GET /schools) y los estudiantes
 * de la que elija (GET /students?schoolId=). El filtro por curso se hace en memoria para
 * que cambiar de chip no vuelva a pedir la lista.
 *
 * BQ 9: también cronometra cuánto tarda el docente en clasificar (ver [SesionClasificacion]).
 */
class ClasificacionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ClasificacionRepository(app)
    private var materiaClave = ""
    private var cargaEstudiantes: Job? = null

    // Lo que el docente editó en la tarjeta (sexo, edad, nivel). Sobrevive a los cambios de filtro.
    private val ediciones = mutableMapOf<String, EstudianteClasificacion>()

    private var sesion: SesionClasificacion? = null
    private var enPrimerPlano = false

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
        cerrarSesion()   // cada institución es una sesión aparte en el reporte
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

    fun clasificar(id: String, nivel: String) {
        actualizar(id) { it.copy(nivelActual = nivel) }
        if (nivel != NIVEL_RETIRADO) sesion?.registrarClasificacion(id)
    }

    // La pantalla avisa cuando queda visible u oculta: el tiempo en segundo plano no se cuenta.
    fun alReanudar() {
        enPrimerPlano = true
        sesion?.reanudar()
    }

    fun alPausar() {
        enPrimerPlano = false
        sesion?.pausar()
    }

    override fun onCleared() {
        cerrarSesion()
        super.onCleared()
    }

    private fun abrirSesion(schoolId: String, estudiantes: Int) {
        cerrarSesion()
        if (estudiantes == 0) return
        sesion = SesionClasificacion(schoolId, estudiantes).also { if (enPrimerPlano) it.reanudar() }
    }

    /** Si el docente clasificó al menos un estudiante, deja la sesión en cola para el backend. */
    private fun cerrarSesion() {
        val terminada = sesion ?: return
        sesion = null
        val cuerpo = terminada.aJson(materiaClave, InfoDispositivo.versionApp(getApplication())) ?: return
        repo.enviarSesion(cuerpo)
    }

    private fun cargarInstituciones() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensaje = null) }
            try {
                val instituciones = repo.misInstituciones()
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
                val remotos = repo.estudiantes(schoolId)
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
                // Un reintento de la misma institución sigue la sesión que ya iba corriendo
                if (sesion?.schoolId != schoolId) abrirSesion(schoolId, estudiantes.size)
            } catch (e: CancellationException) {
                throw e   // el docente cambió de institución antes de que llegara la lista
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, mensaje = mensajeDe(e)) }
            }
        }
    }

    private fun mensajeDe(e: Exception): String = when ((e as? CloudFunctionsApi.ApiException)?.code) {
        "no-school" -> "No tienes instituciones asignadas."
        "unauthenticated" -> "Tu sesión expiró. Vuelve a iniciar sesión."
        "permission-denied" -> "No tienes acceso a esta institución."
        null -> "No pudimos cargar los estudiantes. Revisa tu conexión."
        else -> e.message ?: "No pudimos cargar los estudiantes."
    }
}

/**
 * BQ 9: una pasada del docente por la lista de una institución. Empieza cuando la lista
 * aparece y termina en la última clasificación (no cuando sale de la pantalla), así el rato
 * que se queda mirando al final no infla el tiempo. Solo cuenta el tiempo en primer plano.
 */
internal class SesionClasificacion(val schoolId: String, private val estudiantesEnLista: Int) {
    private val clientId = UUID.randomUUID().toString()
    private val inicioMs = System.currentTimeMillis()
    private var activoMs = 0L
    private var reanudadaEn: Long? = null   // elapsedRealtime; null = pausada
    private var activoHastaUltimaMs = 0L
    private var ultimaClasificacionMs = 0L
    private val clasificados = mutableSetOf<String>()

    private fun activoAhora(): Long = activoMs + (reanudadaEn?.let { SystemClock.elapsedRealtime() - it } ?: 0L)

    fun reanudar() {
        if (reanudadaEn == null) reanudadaEn = SystemClock.elapsedRealtime()
    }

    fun pausar() {
        activoMs = activoAhora()
        reanudadaEn = null
    }

    /** Volver a clasificar al mismo estudiante no lo cuenta dos veces, pero sí alarga el tiempo. */
    fun registrarClasificacion(estudianteId: String) {
        clasificados += estudianteId
        activoHastaUltimaMs = activoAhora()
        ultimaClasificacionMs = System.currentTimeMillis()
    }

    /** Cuerpo de POST /classificationSessions, o null si no clasificó a nadie. */
    fun aJson(materia: String, versionApp: String): JSONObject? {
        if (clasificados.isEmpty()) return null
        return JSONObject()
            .put("clientId", clientId)
            .put("schoolId", schoolId)
            .put("subject", materia)
            .put("platform", InfoDispositivo.PLATAFORMA)
            .put("appVersion", versionApp)
            .put("startedAt", Instant.ofEpochMilli(inicioMs).toString())
            .put("endedAt", Instant.ofEpochMilli(ultimaClasificacionMs).toString())
            .put("activeSeconds", ((activoHastaUltimaMs + 999) / 1000).coerceAtLeast(1))
            .put("studentsClassified", clasificados.size)
            .put("studentsInList", maxOf(estudiantesEnLista, clasificados.size))
    }
}
