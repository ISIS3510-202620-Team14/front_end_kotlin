package com.enad.enadmovil.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.CloudFunctionsApi.DocenteClasificacion
import com.enad.enadmovil.data.remote.CloudFunctionsApi.InstitucionClasificacion
import com.enad.enadmovil.data.repository.AuthRepository
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

private val LOCALE_CO: Locale = Locale.forLanguageTag("es-CO")

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

/** BQ tipo 3 (#9): cuánto tarda en promedio cada docente en clasificar 25 estudiantes, por institución. */
class ReporteClasificacionViewModel(
    private val auth: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReporteClasificacionUiState())
    val uiState: StateFlow<ReporteClasificacionUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun seleccionarMateria(materia: String?) {
        _uiState.update { it.copy(materia = materia) }
        cargar()
    }

    fun cargar() {
        val materia = _uiState.value.materia
        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val nuevo = try {
                val token = auth.obtenerToken()
                if (token == null) {
                    ReporteClasificacionUiState(cargando = false, materia = materia, error = "Inicia sesión de nuevo para ver el reporte.")
                } else {
                    ReporteClasificacionUiState(
                        cargando = false,
                        materia = materia,
                        instituciones = CloudFunctionsApi.reporteClasificacion(token, materia)
                    )
                }
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
            if (_uiState.value.materia == materia) _uiState.value = nuevo
        }
    }
}

@Composable
fun ReporteClasificacionSection(
    modifier: Modifier = Modifier,
    viewModel: ReporteClasificacionViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tiempo de clasificación",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Minutos que tarda cada docente en clasificar 25 estudiantes (últimas 8 semanas).",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = viewModel::cargar, enabled = !estado.cargando) {
                Text("Actualizar")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MATERIAS_REPORTE.forEach { (clave, etiqueta) ->
                FilterChip(
                    selected = estado.materia == clave,
                    onClick = { viewModel.seleccionarMateria(clave) },
                    label = { Text(etiqueta) }
                )
            }
        }

        when {
            estado.cargando -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            estado.error != null -> TarjetaTexto(estado.error.orEmpty())
            estado.instituciones.isEmpty() ->
                TarjetaTexto("Todavía no hay sesiones de clasificación en las últimas 8 semanas.")
            else -> estado.instituciones.forEach { TarjetaInstitucion(it) }
        }
    }
}

@Composable
private fun TarjetaInstitucion(institucion: InstitucionClasificacion) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = institucion.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Promedio de ${institucion.docentes.size} docente(s) · ${institucion.estudiantes} clasificaciones",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = minutosTexto(institucion.promedioMinutosPor25),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = EnadPrimary
            )
        }
        institucion.docentes.forEach { FilaDocente(it) }
    }
}

@Composable
private fun FilaDocente(docente: DocenteClasificacion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnadBackground, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = docente.nombre ?: "Docente sin nombre",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${docente.sesiones} sesión(es) · ${docente.estudiantes} estudiantes",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = minutosTexto(docente.minutosPor25),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun TarjetaTexto(texto: String) {
    Text(
        text = texto,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(18.dp)
    )
}

private fun minutosTexto(minutos: Double): String =
    if (minutos % 1.0 == 0.0) "${minutos.toLong()} min" else String.format(LOCALE_CO, "%.1f min", minutos)
