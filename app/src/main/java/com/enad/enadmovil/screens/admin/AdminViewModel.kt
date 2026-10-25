package com.enad.enadmovil.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.repository.ReportesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Estado de una consulta al backend: cargando, lista o con error para mostrar. */
sealed interface Carga<out T> {
    data object Cargando : Carga<Nothing>
    data class Lista<T>(val datos: T) : Carga<T>
    data class Fallo(val mensaje: String) : Carga<Nothing>
}

val <T> Carga<T>.datos: T? get() = (this as? Carga.Lista<T>)?.datos

data class AdminUiState(
    val semanaAperturas: LocalDate = lunesActual(),
    val aperturas: Carga<CloudFunctionsApi.ReporteAperturas> = Carga.Cargando,
    val horas: Carga<List<CloudFunctionsApi.SemanaHoras>> = Carga.Cargando,
    val actividades: Carga<CloudFunctionsApi.TopActividades> = Carga.Cargando,
    val busqueda: String = "",
    val instituciones: Carga<List<CloudFunctionsApi.EscuelaAdmin>> = Carga.Cargando,
    val docentes: Map<String, Carga<List<CloudFunctionsApi.DocenteAdmin>>> = emptyMap()
)

/**
 * Portal admin (mismo contenido que el de Flutter): Resumen, Instituciones y los reportes de
 * aperturas (BQ #6), horas y actividades más usadas (BQ #11). Clasificación y sincronización
 * tienen su propio ViewModel.
 */
class AdminViewModel(
    private val repo: ReportesRepository = ReportesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()
    private var busquedaJob: Job? = null

    init {
        recargar()
    }

    fun recargar() {
        cargarAperturas()
        cargarHoras()
        cargarActividades()
        cargarInstituciones()
    }

    fun cargarAperturas(forzar: Boolean = false) {
        val semana = _uiState.value.semanaAperturas
        _uiState.update { it.copy(aperturas = Carga.Cargando) }
        viewModelScope.launch {
            val resultado = consultar { repo.aperturas(semana.toString(), forzar) }
            // Si el admin cambió de semana mientras cargaba, esta respuesta ya no aplica
            if (_uiState.value.semanaAperturas == semana) _uiState.update { it.copy(aperturas = resultado) }
        }
    }

    fun cambiarSemanaAperturas(semanas: Long) {
        val nueva = _uiState.value.semanaAperturas.plusWeeks(semanas)
        if (nueva.isAfter(lunesActual())) return
        _uiState.update { it.copy(semanaAperturas = nueva) }
        cargarAperturas()
    }

    fun cargarHoras(forzar: Boolean= false) {
        _uiState.update { it.copy(horas = Carga.Cargando) }
        viewModelScope.launch {
            val resultado = consultar { repo.horas(forzar).sortedByDescending { s -> s.semana } }
            _uiState.update { it.copy(horas = resultado) }
        }
    }

    fun cargarActividades(forzar: Boolean = false) {
        _uiState.update { it.copy(actividades = Carga.Cargando) }
        viewModelScope.launch {
            val resultado = consultar { repo.topActividades(forzar) }
            _uiState.update { it.copy(actividades = resultado) }
        }
    }

    fun buscar(texto: String) {
        _uiState.update { it.copy(busqueda = texto) }
        busquedaJob?.cancel()
        busquedaJob = viewModelScope.launch {
            delay(350) // espera a que el admin deje de escribir
            cargarInstituciones()
        }
    }

    fun cargarInstituciones(forzar: Boolean = false) {
        val busqueda = _uiState.value.busqueda
        _uiState.update { it.copy(instituciones = Carga.Cargando) }
        viewModelScope.launch {
            val resultado = consultar { repo.instituciones(busqueda, forzar) }
            if (_uiState.value.busqueda == busqueda) _uiState.update { it.copy(instituciones = resultado) }
        }
    }

    /** Abre o cierra los docentes de una institución; la primera vez los pide al backend. */
    fun alternarDocentes(schoolId: String) {
        val actual = _uiState.value.docentes
        if (actual.containsKey(schoolId)) {
            _uiState.update { it.copy(docentes = actual - schoolId) }
            return
        }
        _uiState.update { it.copy(docentes = actual + (schoolId to Carga.Cargando)) }
        viewModelScope.launch {
            val resultado = consultar { repo.docentes(schoolId) }
            _uiState.update {
                if (it.docentes.containsKey(schoolId)) it.copy(docentes = it.docentes + (schoolId to resultado)) else it
            }
        }
    }
    fun actualizarAperturas() = cargarAperturas(forzar = true)
    fun actualizarHoras() = cargarHoras(forzar = true)
    fun actualizarActividades() = cargarActividades(forzar = true)

    private suspend fun <T> consultar(bloque: suspend () -> T): Carga<T> =
        try {
            Carga.Lista(bloque())
        } catch (e: CloudFunctionsApi.ApiException) {
            Carga.Fallo(mensajeError(e))
        } catch (e: IOException) {
            Carga.Fallo(SIN_CONEXION)
        }
}

private const val SIN_CONEXION = "No se pudo cargar. Revisa tu conexión e intenta de nuevo."

internal fun mensajeError(e: CloudFunctionsApi.ApiException): String = when (e.status) {
    403 -> "Solo un administrador puede ver este reporte."
    404 -> "Este reporte todavía no está disponible en el servidor."
    else -> e.message ?: SIN_CONEXION
}

/** Lunes de la semana actual en hora de Colombia, como lo calcula el backend. */
internal fun lunesActual(): LocalDate =
    LocalDate.now(ZoneId.of("America/Bogota")).with(DayOfWeek.MONDAY)
