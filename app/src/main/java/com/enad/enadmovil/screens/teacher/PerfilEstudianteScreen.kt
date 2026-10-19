package com.enad.enadmovil.ui.screens.teacher

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.repository.ClasificacionRepository
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PerfilEstudianteUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val progreso: CloudFunctionsApi.ProgresoEstudiante? = null,
    val institucion: String = "Institución sin identificar",
    val sede: String = "Sin sede registrada",
    val grupos: List<Pair<String, String>> = emptyList()
)

/** Carga GET /students/{id}/progress, los grupos del estudiante y los nombres de su IE y sede. */
class PerfilEstudianteViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ClasificacionRepository(app)
    private var estudianteId: String? = null
    private val _uiState = MutableStateFlow(PerfilEstudianteUiState())
    val uiState: StateFlow<PerfilEstudianteUiState> = _uiState.asStateFlow()

    fun iniciar(id: String) {
        if (estudianteId == id) return
        estudianteId = id
        cargar()
    }

    fun cargar() {
        val id = estudianteId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val progreso = repo.progreso(id)
                val e = progreso.estudiante
                val institucion = runCatching { repo.misInstituciones() }.getOrDefault(emptyList())
                    .find { it.id == e.schoolId }
                val grupos = runCatching { repo.gruposDe(e.schoolId, e.id) }.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        cargando = false,
                        progreso = progreso,
                        institucion = institucion?.name ?: "Institución sin identificar",
                        // "campus" guarda el id de la sede; si no está en la lista se muestra tal cual.
                        sede = e.campus?.let { c -> institucion?.campuses?.find { s -> s.id == c }?.name ?: c }
                            ?: "Sin sede registrada",
                        grupos = grupos
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(cargando = false, error = "No pudimos actualizar el perfil.") }
            }
        }
    }
}

private val MATERIAS_PERFIL = listOf("lectura" to "Lectura", "matematicas" to "Matemáticas")

/** Perfil del estudiante (student_profile_view en Flutter). */
@Composable
fun PerfilEstudianteScreen(
    estudianteId: String,
    onBack: () -> Unit,
    onVerHistorial: () -> Unit,
    viewModel: PerfilEstudianteViewModel = viewModel(key = "perfil-$estudianteId")
) {
    LaunchedEffect(estudianteId) { viewModel.iniciar(estudianteId) }
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val e = estado.progreso?.estudiante

    Scaffold(containerColor = EnadBackground, topBar = { BarraConVolver("Perfil del estudiante", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (estado.cargando) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EnadInk)
            if (e != null) {
                EnadPanel {
                    Text(e.fullName, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Linea("Código: ${e.code}")
                    Linea("IE: ${estado.institucion}")
                    Linea("Sede: ${estado.sede}")
                    Linea("Grado ${e.grade}")
                    e.age?.let { Linea("Edad: $it años") }
                }
            }
            estado.error?.let { error ->
                EnadPanel {
                    Text(error, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::cargar) { Text("Reintentar", color = EnadInk) }
                }
            }
            val p = estado.progreso
            if (p != null && !estado.cargando && estado.error == null) {
                EnadPanel {
                    Text("Grupos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    if (estado.grupos.isEmpty()) Linea("Sin grupo asignado")
                    estado.grupos.forEach { (nombre, materia) ->
                        Text(nombre, style = MaterialTheme.typography.bodyLarge)
                        Text(materia, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                EnadPanel {
                    Text("Asistencia registrada", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    if (p.asistenciaRegistrada == 0) {
                        Linea("Sin registros de asistencia")
                    } else {
                        val pct = Math.round(100f * p.asistenciaPresente / p.asistenciaRegistrada)
                        Linea("$pct% · ${p.asistenciaPresente} de ${p.asistenciaRegistrada} sesiones")
                    }
                }
                EnadPanel {
                    Text("Evaluaciones", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    MATERIAS_PERFIL.forEach { (clave, titulo) ->
                        Text(titulo, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            p.estudiante.niveles[clave] ?: "Sin evaluar",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    EnadSecondaryButton("Ver historial", onClick = onVerHistorial, icon = Icons.Outlined.History)
                }
            }
        }
    }
}

/** Historial de evaluación (student_history_view en Flutter): momento, fecha y nivel por materia. */
@Composable
fun HistorialEstudianteScreen(
    estudianteId: String,
    onBack: () -> Unit,
    viewModel: PerfilEstudianteViewModel = viewModel(key = "perfil-$estudianteId")
) {
    LaunchedEffect(estudianteId) { viewModel.iniciar(estudianteId) }
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val p = estado.progreso

    Scaffold(containerColor = EnadBackground, topBar = { BarraConVolver("Historial de evaluación", onBack) }) { padding ->
        when {
            estado.cargando && p == null -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator(color = EnadPrimary) }

            p == null -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No pudimos cargar el historial.", style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = viewModel::cargar) { Text("Volver a intentar", color = EnadInk) }
            }

            else -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(p.estudiante.fullName, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cada registro muestra el momento, la fecha y el nivel. Las correcciones se guardan como nuevos registros.",
                    style = MaterialTheme.typography.bodyMedium
                )
                MATERIAS_PERFIL.forEach { (clave, titulo) ->
                    Spacer(Modifier.height(24.dp))
                    Text(titulo, style = MaterialTheme.typography.titleLarge)
                    val registros = p.historial[clave].orEmpty()
                    if (registros.isEmpty()) {
                        Text("Todavía no hay evaluaciones guardadas.", style = MaterialTheme.typography.bodyMedium)
                    }
                    val materia = if (clave == "lectura") MATERIA_LECTURA else MATERIA_MATEMATICAS
                    registros.asReversed().forEach { r ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(MOMENTOS_EVALUACION[r.tipo] ?: "Evaluación", style = MaterialTheme.typography.bodyLarge)
                                Text(r.fecha, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.width(12.dp))
                            InsigniaNivel(r.nivel, materia)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Linea(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium)
}
