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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.CloudFunctionsApi.ConteoRevision
import com.enad.enadmovil.data.remote.CloudFunctionsApi.RevisionEscaneo
import com.enad.enadmovil.data.remote.CloudFunctionsApi.TotalesRevision
import com.enad.enadmovil.data.repository.ReportesRepository
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val CAMPOS = mapOf("name" to "el nombre", "code" to "el código")
private val ORIGENES = listOf("attendance" to "Desde Asistencia", "manage_students" to "Desde Gestionar estudiantes")

data class ReporteRevisionEscaneoUiState(
    val cargando: Boolean = true,
    val reporte: RevisionEscaneo? = null,
    val error: String? = null
)

/** BQ tipo 3 (#11): de las filas que leyó el escáner, qué dejó, corrigió o borró el docente antes de importar. */
class ReporteRevisionEscaneoViewModel(
    private val repo: ReportesRepository = ReportesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReporteRevisionEscaneoUiState())
    val uiState: StateFlow<ReporteRevisionEscaneoUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar(forzar: Boolean = false) {
        _uiState.value = _uiState.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            _uiState.value = try {
                ReporteRevisionEscaneoUiState(cargando = false, reporte = repo.revisionEscaneo(forzar))
            } catch (e: CloudFunctionsApi.ApiException) {
                val mensaje = when (e.status) {
                    403 -> "Solo un administrador puede ver este reporte."
                    404 -> "Este reporte todavía no está disponible en el servidor."
                    else -> e.message
                }
                ReporteRevisionEscaneoUiState(cargando = false, error = mensaje)
            } catch (e: Exception) {
                ReporteRevisionEscaneoUiState(cargando = false, error = "No se pudo cargar el reporte. Revisa tu conexión e intenta de nuevo.")
            }
        }
    }

    fun actualizar() = cargar(forzar = true)
}

@Composable
fun ReporteRevisionEscaneoSection(viewModel: ReporteRevisionEscaneoViewModel = viewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Revisión del escaneo",
            descripcion = "Qué corrigen los docentes en las filas que leyó el escáner antes de importar (últimas 8 semanas).",
            cargando = estado.cargando,
            onActualizar = viewModel::actualizar
        )
        val carga: Carga<RevisionEscaneo?> = when {
            estado.cargando -> Carga.Cargando
            estado.error != null -> Carga.Fallo(estado.error.orEmpty())
            else -> Carga.Lista(estado.reporte)
        }
        ContenidoCarga(
            carga = carga,
            vacio = { it == null || it.total.importaciones == 0 },
            textoVacio = "Todavía no hay listas escaneadas importadas en las últimas 8 semanas."
        ) { reporte ->
            if (reporte != null) {
                PanelResumen(reporte)
                ORIGENES.forEach { (clave, titulo) ->
                    reporte.porOrigen[clave]?.takeIf { it.importaciones > 0 }?.let { PanelTotales(titulo, it) }
                }
            }
        }
    }
}

/** La respuesta de la BQ: cuánto se corrige y qué campo se corrige más. */
@Composable
private fun PanelResumen(reporte: RevisionEscaneo) {
    val total = reporte.total
    EnadPanel {
        Text("Filas leídas por el escáner", style = MaterialTheme.typography.titleMedium)
        Text(
            "${total.leidas} fila(s) en ${total.importaciones} importación(es) de ${reporte.docentes} docente(s)",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            reporte.masCorregido?.let { "El campo que más se corrige es ${CAMPOS[it] ?: it}." }
                ?: "Ningún campo se corrige más que el otro.",
            style = MaterialTheme.typography.labelLarge,
            color = EnadPrimary
        )
        Filas(total)
    }
}

@Composable
private fun PanelTotales(titulo: String, totales: TotalesRevision) {
    EnadPanel {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Text("${totales.leidas} fila(s) en ${totales.importaciones} importación(es)", style = MaterialTheme.typography.bodyMedium)
        Filas(totales)
    }
}

@Composable
private fun Filas(totales: TotalesRevision) {
    listOf(
        "Sin cambios" to totales.sinCambios,
        "Corregidas" to totales.editadas,
        "Borradas" to totales.borradas,
        "Nombre corregido" to totales.nombres,
        "Código corregido" to totales.codigos
    ).forEach { (etiqueta, conteo) ->
        Spacer(Modifier.height(8.dp))
        FilaConteo(etiqueta, conteo)
    }
}

@Composable
private fun FilaConteo(etiqueta: String, conteo: ConteoRevision) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnadBackground, ControlShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium)
            Text("${conteo.filas} fila(s)", style = MaterialTheme.typography.bodyMedium)
        }
        Text(porcentajeTexto(conteo.porcentaje), style = MaterialTheme.typography.labelLarge)
    }
}
