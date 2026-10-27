package com.enad.enadmovil.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.CloudFunctionsApi.DocenteClasificacion
import com.enad.enadmovil.data.remote.CloudFunctionsApi.InstitucionClasificacion
import com.enad.enadmovil.data.repository.ReportesRepository
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Materia del filtro: clave del backend -> etiqueta (null = las dos).
private val MATERIAS_REPORTE = listOf<Pair<String?, String>>(
    null to "Todas", "lectura" to "Lectura", "matematicas" to "Matemáticas"
)

data class ReporteClasificacionUiState(
    val cargando: Boolean = true,
    val materia: String? = null,
    val instituciones: List<InstitucionClasificacion> = emptyList(),
    val error: String? = null
)

/** Cada cuánto se vuelve a pedir el reporte mientras el admin lo tiene en pantalla. */
private const val REFRESCO_MS = 30_000L

/** BQ tipo 3 (#9): cuánto tarda en promedio cada docente en clasificar 25 estudiantes, por institución. */
class ReporteClasificacionViewModel(
    private val repo: ReportesRepository = ReportesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReporteClasificacionUiState())
    val uiState: StateFlow<ReporteClasificacionUiState> = _uiState.asStateFlow()

    private var refresco: Job? = null

    /**
     * Mientras el reporte está visible se recarga al aparecer y luego cada [REFRESCO_MS], así el
     * admin ve las sesiones que los docentes van mandando sin tocar Actualizar.
     */
    fun iniciarRefresco() {
        if (refresco?.isActive == true) return
        refresco = viewModelScope.launch {
            while (true) {
                cargar(forzar = true, silencioso = true)
                delay(REFRESCO_MS)
            }
        }
    }

    fun detenerRefresco() {
        refresco?.cancel()
        refresco = null
    }

    fun seleccionarMateria(materia: String?) {
        _uiState.update { it.copy(materia = materia) }
        cargar()
    }

    /** [silencioso]: si ya hay datos en pantalla no se muestra el spinner ni se cambian por un error. */
    fun cargar(forzar: Boolean = false, silencioso: Boolean = false) {
        val materia = _uiState.value.materia
        val conDatos = _uiState.value.let { !it.cargando && it.error == null }
        val enSegundoPlano = silencioso && conDatos
        if (!enSegundoPlano) _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val nuevo = try {
                ReporteClasificacionUiState(
                    cargando = false,
                    materia = materia,
                    instituciones = repo.tiempoClasificacion(materia, forzar)
                )
            } catch (e: CloudFunctionsApi.ApiException) {
                val mensaje = when (e.status) {
                    403 -> "Solo un administrador puede ver este reporte."
                    404 -> "Este reporte todavía no está disponible en el servidor."
                    else -> e.message
                }
                ReporteClasificacionUiState(cargando = false, materia = materia, error = mensaje)
            } catch (e: Exception) {
                ReporteClasificacionUiState(cargando = false, materia = materia, error = "No se pudo cargar el reporte. Revisa tu conexión e intenta de nuevo.")
            }
            // Si el admin cambió de materia mientras cargaba, esta respuesta ya no aplica
            if (_uiState.value.materia != materia) return@launch
            // Un refresco automático que falla deja lo que ya se veía
            if (enSegundoPlano && nuevo.error != null) return@launch
            _uiState.value = nuevo
        }
    }

    fun actualizar() = cargar(forzar = true)
}

@Composable
fun ReporteClasificacionSection(viewModel: ReporteClasificacionViewModel = viewModel()) {
    // Solo se refresca mientras el reporte está en pantalla y la app en primer plano.
    LifecycleResumeEffect(viewModel) {
        viewModel.iniciarRefresco()
        onPauseOrDispose { viewModel.detenerRefresco() }
    }
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Tiempo de clasificación",
            descripcion = "Minutos que tarda cada docente en clasificar 25 estudiantes (últimas 8 semanas). Se actualiza solo cada 30 s.",
            cargando = estado.cargando,
            onActualizar = viewModel::actualizar
        )
        AdminChips(
            opciones = MATERIAS_REPORTE.map { it.second },
            seleccionada = MATERIAS_REPORTE.indexOfFirst { it.first == estado.materia },
            onSeleccionar = { viewModel.seleccionarMateria(MATERIAS_REPORTE[it].first) }
        )
        val carga: Carga<List<InstitucionClasificacion>> = when {
            estado.cargando -> Carga.Cargando
            estado.error != null -> Carga.Fallo(estado.error.orEmpty())
            else -> Carga.Lista(estado.instituciones)
        }
        ContenidoCarga(
            carga = carga,
            vacio = { it.isEmpty() },
            textoVacio = "Todavía no hay sesiones de clasificación en las últimas 8 semanas."
        ) { instituciones ->
            instituciones.forEach { TarjetaInstitucion(it) }
        }
    }
}

@Composable
private fun TarjetaInstitucion(institucion: InstitucionClasificacion) {
    EnadPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(institucion.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Promedio de ${institucion.docentes.size} docente(s) · ${institucion.estudiantes} clasificaciones",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(minutosTexto(institucion.promedioMinutosPor25), style = MaterialTheme.typography.headlineSmall, color = EnadPrimary)
        }
        institucion.docentes.forEach {
            Spacer(Modifier.height(8.dp))
            FilaDocente(it)
        }
    }
}

@Composable
private fun FilaDocente(docente: DocenteClasificacion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnadBackground, ControlShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(docente.nombre ?: "Docente sin nombre", style = MaterialTheme.typography.labelMedium)
            Text("${docente.sesiones} sesión(es) · ${docente.estudiantes} estudiantes", style = MaterialTheme.typography.bodyMedium)
        }
        Text(minutosTexto(docente.minutosPor25), style = MaterialTheme.typography.labelLarge)
    }
}

private fun minutosTexto(minutos: Double): String =
    if (minutos % 1.0 == 0.0) "${minutos.toLong()} min" else String.format(LOCALE_CO, "%.1f min", minutos)
