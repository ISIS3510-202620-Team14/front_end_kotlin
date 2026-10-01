package com.enad.enadmovil.ui.screens.teacher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.local.entity.EstadoRemoto
import com.enad.enadmovil.data.local.entity.EstudianteEntity
import com.enad.enadmovil.data.repository.AsistenciaRepository
import com.enad.enadmovil.data.repository.AuthRepository
import com.enad.enadmovil.data.sync.SyncScheduler
import com.enad.enadmovil.domain.model.EstudianteEscaneado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class AsistenciaUiState(
    val cargando: Boolean = true,
    val sinColegio: Boolean = false,
    val estudiantes: List<EstudianteAsistencia> = emptyList(),
    val roster: List<EstudianteSalon> = emptyList(),
    val pendientesSync: Int = 0,
    val mensaje: String? = null
)

private data class Filtros(val grado: Int?, val fecha: String)
private data class Meta(val cargando: Boolean = true, val sinColegio: Boolean = false, val mensaje: String? = null)

/** Lunes a viernes de la semana actual, con su fecha real. */
internal fun diasDeLaSemana(hoy: LocalDate = LocalDate.now()): List<DiaSemana> {
    val lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes").mapIndexed { i, nombre ->
        val fecha = lunes.plusDays(i.toLong())
        DiaSemana(nombre.take(3), fecha.dayOfMonth, nombre, fecha)
    }
}

/** Hoy si es día de semana; si no, el lunes. */
internal fun diaInicial(dias: List<DiaSemana>, hoy: LocalDate = LocalDate.now()): DiaSemana =
    dias.firstOrNull { it.fecha == hoy } ?: dias.first()

@OptIn(ExperimentalCoroutinesApi::class)
class AsistenciaViewModel(app: Application) : AndroidViewModel(app) {

    private val contexto = app
    private val repo = AsistenciaRepository(app)
    private val auth = AuthRepository()
    private val uid = auth.uidActual().orEmpty()

    private val schoolId = MutableStateFlow<String?>(null)
    private val meta = MutableStateFlow(Meta())
    private val filtros = MutableStateFlow(
        Filtros(grado = null, fecha = diaInicial(diasDeLaSemana()).fecha.toString())
    )

    private val estudiantesFlow = schoolId.flatMapLatest { sid ->
        if (sid == null) flowOf(emptyList<EstudianteEntity>()) else repo.observarEstudiantes(sid)
    }
    private val marcasFlow = filtros.map { it.fecha }.distinctUntilChanged()
        .flatMapLatest { repo.observarAsistencia(it) }
    private val pendientesFlow = repo.observarPendientes(uid)

    val uiState: StateFlow<AsistenciaUiState> = combine(
        estudiantesFlow, marcasFlow, filtros, pendientesFlow, meta
    ) { alumnos, marcas, f, pendientes, m ->
        val marcaPorId = marcas.associateBy { it.estudianteId }
        // Se ven los del curso elegido y, además, cualquiera que ya tenga marca ese día (los "inesperados").
        val visibles = alumnos.filter { a ->
            (f.grado == null || a.grade == f.grado) ||
                    (marcaPorId[a.id]?.estado ?: EstadoRemoto.SIN_REGISTRO) != EstadoRemoto.SIN_REGISTRO
        }
        AsistenciaUiState(
            cargando = m.cargando,
            sinColegio = m.sinColegio,
            estudiantes = visibles.mapIndexed { i, a ->
                EstudianteAsistencia(
                    id = a.id,
                    numero = i + 1,
                    nombre = a.fullName,
                    etiqueta = etiqueta(a),
                    estado = aEstado(marcaPorId[a.id]?.estado)
                )
            },
            roster = alumnos.map { EstudianteSalon(it.id, it.fullName, it.grade?.let { g -> "Grado $g" } ?: "—") },
            pendientesSync = pendientes,
            mensaje = m.mensaje
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AsistenciaUiState())

    init {
        viewModelScope.launch {
            val sid = auth.schoolIdActual()
            if (sid == null) {
                meta.update { it.copy(cargando = false, sinColegio = true) }
            } else {
                schoolId.value = sid
                meta.update { it.copy(cargando = false) }
                SyncScheduler.programar(contexto) // sube lo que haya quedado pendiente de antes
                refrescar()
            }
        }
    }

    private fun refrescar() {
        val sid = schoolId.value ?: return
        viewModelScope.launch { repo.descargarRoster(sid, uid, filtros.value.fecha) }
    }

    fun seleccionarGrado(grado: Int?) = filtros.update { it.copy(grado = grado) }

    fun seleccionarFecha(fecha: LocalDate) {
        filtros.update { it.copy(fecha = fecha.toString()) }
        refrescar()
    }

    fun marcar(id: String, estado: EstadoAsistencia) {
        viewModelScope.launch { repo.marcar(id, filtros.value.fecha, aRemoto(estado), uid) }
    }

    fun limpiar() {
        val ids = uiState.value.estudiantes.map { it.id }
        viewModelScope.launch { repo.limpiarDia(ids, filtros.value.fecha, uid) }
    }

    /** Todo ya está guardado en el teléfono; esto solo pide subirlo ahora (o en cuanto haya internet). */
    fun guardar() = SyncScheduler.programar(contexto)

    fun importar(filas: List<EstudianteEscaneado>) {
        val sid = schoolId.value ?: return avisar("Tu cuenta no tiene colegio asignado.")
        viewModelScope.launch {
            val r = repo.guardarLista(sid, uid, filas, filtros.value.grado)
            val partes = mutableListOf("Nuevos: ${r.nuevos}. Ya estaban en la lista: ${r.existentes}.")
            if (r.sinGrado > 0) {
                partes += "${r.sinGrado} no tienen grado: elige el curso (Grado 3, 4 o 5) arriba y vuelve a importar."
            }
            avisar(partes.joinToString(" "))
        }
    }

    fun agregarInesperado(id: String) = marcar(id, EstadoAsistencia.ASISTIO)

    fun crearInesperado(nombre: String, edad: Int?) {
        val sid = schoolId.value ?: return avisar("Tu cuenta no tiene colegio asignado.")
        val grado = filtros.value.grado
            ?: return avisar("Elige un curso (Grado 3, 4 o 5) para crear el estudiante.")
        viewModelScope.launch { repo.crearInesperado(sid, uid, nombre, edad, grado, filtros.value.fecha) }
    }

    fun consumirMensaje() = meta.update { it.copy(mensaje = null) }

    private fun avisar(texto: String) = meta.update { it.copy(mensaje = texto) }

    private fun etiqueta(a: EstudianteEntity): String =
        listOfNotNull(a.code ?: "Código pendiente", a.grade?.let { "Gr. $it" }, a.age?.let { "$it años" })
            .joinToString(" · ")

    private fun aEstado(remoto: String?): EstadoAsistencia = when (remoto) {
        EstadoRemoto.VINO -> EstadoAsistencia.ASISTIO
        EstadoRemoto.NO_VINO -> EstadoAsistencia.NO_ASISTIO
        else -> EstadoAsistencia.PENDIENTE
    }

    private fun aRemoto(estado: EstadoAsistencia): String = when (estado) {
        EstadoAsistencia.ASISTIO -> EstadoRemoto.VINO
        EstadoAsistencia.NO_ASISTIO -> EstadoRemoto.NO_VINO
        EstadoAsistencia.PENDIENTE -> EstadoRemoto.SIN_REGISTRO
    }
}