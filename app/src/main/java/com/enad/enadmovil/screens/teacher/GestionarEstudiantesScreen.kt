package com.enad.enadmovil.ui.screens.teacher

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.repository.AsistenciaRepository
import com.enad.enadmovil.data.repository.AuthRepository
import com.enad.enadmovil.data.repository.ClasificacionRepository
import com.enad.enadmovil.domain.model.EstudianteEscaneado
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadPrimaryButton
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.components.EnadTextField
import com.enad.enadmovil.ui.screens.teacher.scan.EscanerListaSheet
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GestionarEstudiantesUiState(
    val instituciones: List<Pair<String, String>> = emptyList(),   // id -> nombre
    val schoolId: String? = null,
    val grado: Int = 3,
    val ocupado: Boolean = false,
    val mensaje: String? = null
)

/**
 * "Gestionar estudiantes" de Flutter (student_roster_view). Los estudiantes se guardan en Room y el
 * SyncWorker los sube; sin código quedan provisionales y el servidor les asigna uno.
 */
class GestionarEstudiantesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AsistenciaRepository(app)
    private val clasificacion = ClasificacionRepository(app)
    private val auth = AuthRepository()
    private val _uiState = MutableStateFlow(GestionarEstudiantesUiState())
    val uiState: StateFlow<GestionarEstudiantesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val propia = runCatching { auth.schoolIdActual() }.getOrNull()
            val lista = runCatching { clasificacion.misInstituciones() }.getOrDefault(emptyList()).map { it.id to it.name }
            val elegida = lista.firstOrNull { it.first == propia }?.first ?: lista.firstOrNull()?.first ?: propia
            _uiState.update { it.copy(instituciones = lista, schoolId = elegida) }
        }
    }

    fun seleccionarInstitucion(id: String) = _uiState.update { it.copy(schoolId = id, mensaje = null) }

    fun seleccionarGrado(grado: Int) = _uiState.update { it.copy(grado = grado, mensaje = null) }

    fun importar(filas: List<EstudianteEscaneado>, alTerminar: (Boolean) -> Unit = {}) {
        val estado = _uiState.value
        val sid = estado.schoolId ?: return _uiState.update { it.copy(mensaje = "Elige una institución.") }
        val uid = auth.uidActual() ?: return
        if (estado.ocupado) return
        _uiState.update { it.copy(ocupado = true, mensaje = null) }
        viewModelScope.launch {
            val r = runCatching { repo.guardarLista(sid, uid, filas, estado.grado) }.getOrNull()
            val provisional = filas.size == 1 && filas.first().codigo == null
            _uiState.update {
                it.copy(
                    ocupado = false,
                    mensaje = when {
                        r == null -> "No pudimos guardar los estudiantes. Vuelve a intentarlo sin cambiar los datos."
                        provisional && r.nuevos == 1 ->
                            "Estudiante registrado con código provisional. Coordinación deberá completar su código oficial."
                        else -> "${r.nuevos} estudiantes importados; ${r.existentes} códigos ya existentes."
                    }
                )
            }
            alTerminar(r != null && r.nuevos > 0)
        }
    }
}

@Composable
fun GestionarEstudiantesScreen(
    onBack: () -> Unit,
    inesperado: Boolean = false,
    viewModel: GestionarEstudiantesViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var nombre by rememberSaveable { mutableStateOf("") }
    var codigo by rememberSaveable { mutableStateOf("") }
    var validar by remember { mutableStateOf(false) }
    var mostrarEscaner by remember { mutableStateOf(false) }
    val habilitado = !estado.ocupado && estado.schoolId != null

    Scaffold(containerColor = EnadBackground, topBar = { BarraConVolver("Estudiantes", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Gestionar estudiantes", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Añade la identificación de estudiantes nuevos. La asistencia se registra por separado.",
                style = MaterialTheme.typography.bodyLarge
            )
            if (inesperado) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Si no conoces el código, déjalo vacío. Se asignará uno provisional para no detener la sesión.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(Modifier.height(24.dp))
            EnadSecondaryButton(
                text = "Añadir estudiantes con escáner",
                onClick = { mostrarEscaner = true },
                icon = Icons.Outlined.DocumentScanner,
                enabled = habilitado
            )
            Spacer(Modifier.height(16.dp))
            Desplegable(
                etiqueta = "Institución",
                valor = estado.instituciones.firstOrNull { it.first == estado.schoolId }?.second.orEmpty(),
                opciones = estado.instituciones,
                onSeleccionar = { if (!estado.ocupado) viewModel.seleccionarInstitucion(it) }
            )
            Spacer(Modifier.height(16.dp))
            Desplegable(
                etiqueta = "Curso",
                valor = "Grado ${estado.grado}",
                opciones = (3..5).map { "$it" to "Grado $it" },
                onSeleccionar = { g -> if (!estado.ocupado) g.toIntOrNull()?.let(viewModel::seleccionarGrado) }
            )
            Spacer(Modifier.height(44.dp))
            EnadPanel {
                Text("Añadir estudiante", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                val faltaNombre = validar && nombre.isBlank()
                EnadTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = "Nombre completo",
                    enabled = !estado.ocupado,
                    isError = faltaNombre,
                    supportingText = if (faltaNombre) "Completa este campo." else null
                )
                Spacer(Modifier.height(16.dp))
                val faltaCodigo = validar && !inesperado && codigo.isBlank()
                EnadTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = "Código del estudiante",
                    enabled = !estado.ocupado,
                    isError = faltaCodigo,
                    supportingText = if (faltaCodigo) "Completa este campo." else "Usa el código de la lista oficial. No inventes uno.",
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                EnadPrimaryButton(
                    text = "Añadir estudiante",
                    enabled = habilitado,
                    onClick = {
                        validar = true
                        if (nombre.isBlank() || (!inesperado && codigo.isBlank())) return@EnadPrimaryButton
                        viewModel.importar(
                            listOf(EstudianteEscaneado(nombre = nombre.trim(), codigo = codigo.trim().ifBlank { null }))
                        ) { creado ->
                            if (creado) {
                                nombre = ""
                                codigo = ""
                                validar = false
                            }
                        }
                    }
                )
            }
            if (estado.ocupado) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = EnadPrimary, trackColor = EnadLine)
            }
            estado.mensaje?.let {
                Spacer(Modifier.height(16.dp))
                EnadPanel { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }

    if (mostrarEscaner) {
        EscanerListaSheet(
            onImportar = { filas ->
                mostrarEscaner = false
                viewModel.importar(filas)
            },
            onCancelar = { mostrarEscaner = false },
            exigirCodigo = !inesperado
        )
    }
}
