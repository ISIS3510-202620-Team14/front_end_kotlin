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
import com.enad.enadmovil.data.remote.CloudFunctionsApi.PasoGuiaConteo
import com.enad.enadmovil.data.remote.CloudFunctionsApi.PasosGuias
import com.enad.enadmovil.data.remote.CloudFunctionsApi.ResumenGuia
import com.enad.enadmovil.data.repository.ReportesRepository
import com.enad.enadmovil.feature.ayuda.Guia
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReportePasosGuiasUiState(
    val cargando: Boolean = true,
    val reporte: PasosGuias? = null,
    val error: String? = null
)

/** BQ tipo 3: en qué paso de cada guía de Ayuda abandonan los docentes y qué pasos se saltan. */
class ReportePasosGuiasViewModel(
    private val repo: ReportesRepository = ReportesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportePasosGuiasUiState())
    val uiState: StateFlow<ReportePasosGuiasUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar(forzar: Boolean = false) {
        _uiState.value = _uiState.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            _uiState.value = try {
                ReportePasosGuiasUiState(cargando = false, reporte = repo.pasosGuias(forzar))
            } catch (e: CloudFunctionsApi.ApiException) {
                val mensaje = when (e.status) {
                    403 -> "Solo un administrador puede ver este reporte."
                    404 -> "Este reporte todavía no está disponible en el servidor."
                    else -> e.message
                }
                ReportePasosGuiasUiState(cargando = false, error = mensaje)
            } catch (e: Exception) {
                ReportePasosGuiasUiState(cargando = false, error = "No se pudo cargar el reporte. Revisa tu conexión e intenta de nuevo.")
            }
        }
    }

    fun actualizar() = cargar(forzar = true)
}

@Composable
fun ReportePasosGuiasSection(viewModel: ReportePasosGuiasViewModel = viewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Guías de Ayuda",
            descripcion = "En qué paso dejan los docentes cada guía y qué pasos opcionales se saltan (últimas ${estado.reporte?.semanas ?: 8} semanas).",
            cargando = estado.cargando,
            onActualizar = viewModel::actualizar
        )
        val carga: Carga<PasosGuias?> = when {
            estado.cargando -> Carga.Cargando
            estado.error != null -> Carga.Fallo(estado.error.orEmpty())
            else -> Carga.Lista(estado.reporte)
        }
        ContenidoCarga(
            carga = carga,
            vacio = { it == null || it.guias.all { g -> g.corridas == 0 } },
            textoVacio = "Todavía nadie ha usado las guías en este periodo."
        ) { reporte ->
            reporte?.guias?.forEach { PanelGuia(it) }
        }
    }
}

@Composable
private fun PanelGuia(resumen: ResumenGuia) {
    val guia = guiaDeId(resumen.id)
    EnadPanel {
        Text(guia?.titulo ?: resumen.id, style = MaterialTheme.typography.titleMedium)
        Text(
            "${resumen.corridas} iniciada(s), ${resumen.completadas} terminada(s) (${porcentajeTexto(resumen.porcentajeCompletadas)})",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Kotlin ${resumen.porPlataforma["kotlin"] ?: 0} · Flutter ${resumen.porPlataforma["flutter"] ?: 0}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        if (resumen.corridas < MINIMO_CORRIDAS_GUIA) {
            Text("Todavía no hay datos suficientes para sacar una conclusión.", style = MaterialTheme.typography.bodyMedium)
        } else {
            ConclusionGuia(guia, resumen)
        }
    }
}

@Composable
private fun ConclusionGuia(guia: Guia?, resumen: ResumenGuia) {
    Column {
        // La respuesta de la BQ: el paso donde más la dejan y el que más se salta
        val abandono = resumen.abandonoPorPaso.firstOrNull()
        Text(
            abandono?.let { "Donde más la dejan: ${nombrePaso(guia, it.paso)}." } ?: "Nadie la dejó a la mitad.",
            style = MaterialTheme.typography.labelLarge,
            color = EnadPrimary
        )
        resumen.pasosOmitidos.firstOrNull()?.let {
            Text("El paso que más se salta: ${nombrePaso(guia, it.paso)}.", style = MaterialTheme.typography.labelLarge, color = EnadPrimary)
        }
        resumen.abandonoPorPaso.take(3).forEach {
            Spacer(Modifier.height(8.dp))
            FilaPaso(guia, it, "la dejaron aquí", resumen.corridas)
        }
        resumen.pasosOmitidos.take(3).forEach {
            Spacer(Modifier.height(8.dp))
            FilaPaso(guia, it, "lo saltaron", resumen.corridas)
        }
    }
}

@Composable
private fun FilaPaso(guia: Guia?, conteo: PasoGuiaConteo, accion: String, corridas: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnadBackground, ControlShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(nombrePaso(guia, conteo.paso), style = MaterialTheme.typography.labelMedium)
            Text("${conteo.corridas} corrida(s) $accion", style = MaterialTheme.typography.bodyMedium)
        }
        Text(porcentajeTexto(Math.round(conteo.corridas * 1000.0 / corridas) / 10.0), style = MaterialTheme.typography.labelLarge)
    }
}
