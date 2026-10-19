package com.enad.enadmovil.feature.misdatos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.local.entity.ReporteHorasEntity
import com.enad.enadmovil.data.local.entity.SyncStatus
import com.enad.enadmovil.data.remote.FirebaseModule
import com.enad.enadmovil.data.repository.AsistenciaRepository
import com.enad.enadmovil.data.repository.AuthRepository
import com.enad.enadmovil.data.repository.ClasificacionRepository
import com.enad.enadmovil.data.repository.GruposRepository
import com.enad.enadmovil.data.repository.HorasRepository
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.Grupo
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Una institución del docente con las sedes que tiene asignadas. */
data class InstitucionDocente(val nombre: String, val municipio: String?, val sedes: List<String>)

data class IndicadoresDocente(
    val cargando: Boolean = false,
    val cargados: Boolean = false,
    val error: String? = null,
    val asistenciaRegistrada: Int = 0,
    val asistenciaPresente: Int = 0,
    val activos: Int = 0,
    /** materia (clave del back) -> nivel -> cantidad de estudiantes activos con ese nivel actual. */
    val niveles: Map<String, Map<String, Int>> = emptyMap()
)

data class MisDatosUiState(
    val nombre: String = "",
    val correo: String = "",
    val instituciones: List<InstitucionDocente> = emptyList(),
    val guardandoNombre: Boolean = false,
    val mensajeNombre: String? = null,
    val grupos: List<Grupo> = emptyList(),
    val reportes: List<ReporteHorasEntity> = emptyList(),
    val indicadores: IndicadoresDocente = IndicadoresDocente(),
    val pendientesAsistencia: Map<String, Int>? = null
)

/** Mis datos, como profile_view en Flutter: perfil, grupos, reportes de horas, indicadores y pendientes. */
class MisDatosViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = AuthRepository()
    private val clasificacion = ClasificacionRepository(app)
    private val grupos = GruposRepository(app)
    private val horas = HorasRepository(app)
    private val asistencia = AsistenciaRepository(app)
    private val local = MutableStateFlow(MisDatosUiState())

    val uiState: StateFlow<MisDatosUiState> = combine(
        local,
        grupos.observarGrupos(AreaMateria.MATEMATICAS),
        grupos.observarGrupos(AreaMateria.LECTURA),
        horas.observarReportesRecientes()
    ) { estado, mate, lect, reportes ->
        estado.copy(grupos = mate + lect, reportes = reportes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MisDatosUiState())

    init {
        cargarPerfil()
        recargarPendientes()
    }

    private fun cargarPerfil() {
        val usuario = auth.usuarioActual() ?: return
        local.update { it.copy(correo = usuario.email.orEmpty(), nombre = usuario.displayName.orEmpty()) }
        viewModelScope.launch {
            val doc = runCatching {
                FirebaseModule.firestore.collection("users").document(usuario.uid).get().await()
            }.getOrNull()
            val sedesAsignadas = (doc?.get("campusIds") as? Map<*, *>).orEmpty()
            doc?.getString("fullName")?.takeIf { it.isNotBlank() }?.let { n -> local.update { it.copy(nombre = n) } }
            doc?.getString("email")?.takeIf { it.isNotBlank() }?.let { c -> local.update { it.copy(correo = c) } }
            val instituciones = runCatching { clasificacion.misInstituciones() }.getOrDefault(emptyList()).map { i ->
                val mias = (sedesAsignadas[i.id] as? List<*>)?.filterIsInstance<String>().orEmpty()
                InstitucionDocente(i.name, i.municipality, i.campuses.filter { it.id in mias }.map { it.name })
            }
            local.update { it.copy(instituciones = instituciones) }
        }
    }

    fun cambiarNombre(nombre: String) = local.update { it.copy(nombre = nombre, mensajeNombre = null) }

    /** users/{uid}.fullName: las reglas de Firestore dejan que el docente cambie solo ese campo. */
    fun guardarNombre() {
        val nombre = local.value.nombre.trim()
        val usuario = auth.usuarioActual() ?: return
        if (nombre.isEmpty()) {
            local.update { it.copy(mensajeNombre = "Escribe tu nombre.") }
            return
        }
        local.update { it.copy(guardandoNombre = true, mensajeNombre = null) }
        viewModelScope.launch {
            val ok = runCatching {
                FirebaseModule.firestore.collection("users").document(usuario.uid).update("fullName", nombre).await()
                usuario.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(nombre).build()).await()
            }.isSuccess
            local.update {
                it.copy(guardandoNombre = false, mensajeNombre = if (ok) "Nombre guardado." else "No pudimos guardar el nombre.")
            }
        }
    }

    /** Se pide al abrir "Mis indicadores": recorre el progreso de cada estudiante activo de a cinco. */
    fun cargarIndicadores() {
        if (local.value.indicadores.cargando) return
        local.update { it.copy(indicadores = it.indicadores.copy(cargando = true, error = null)) }
        viewModelScope.launch {
            val resultado = runCatching {
                val activos = clasificacion.todosLosEstudiantes().filter { !it.retired }
                var registrada = 0
                var presente = 0
                activos.chunked(5).forEach { lote ->
                    lote.map { e -> async { clasificacion.progreso(e.id) } }.awaitAll().forEach { p ->
                        registrada += p.asistenciaRegistrada
                        presente += p.asistenciaPresente
                    }
                }
                val niveles = listOf("lectura", "matematicas").associateWith { materia ->
                    activos.mapNotNull { it.niveles[materia] }.groupingBy { it }.eachCount()
                }
                IndicadoresDocente(
                    cargados = true,
                    asistenciaRegistrada = registrada,
                    asistenciaPresente = presente,
                    activos = activos.size,
                    niveles = niveles
                )
            }
            local.update {
                it.copy(indicadores = resultado.getOrElse { _ ->
                    it.indicadores.copy(cargando = false, error = "No pudimos cargar la asistencia histórica.")
                })
            }
        }
    }

    fun recargarPendientes() {
        val uid = auth.uidActual() ?: return
        local.update { it.copy(pendientesAsistencia = null) }
        viewModelScope.launch {
            val pendientes = runCatching { asistencia.pendientesPorFecha(uid) }.getOrDefault(emptyMap())
            local.update { it.copy(pendientesAsistencia = pendientes) }
        }
    }

    fun cerrarSesion() = auth.cerrarSesion()

    companion object {
        fun enviado(r: ReporteHorasEntity) = r.syncStatus == SyncStatus.SYNCED
    }
}
