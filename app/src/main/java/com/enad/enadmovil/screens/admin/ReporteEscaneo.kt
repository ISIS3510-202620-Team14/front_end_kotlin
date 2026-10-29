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
import com.enad.enadmovil.data.remote.CloudFunctionsApi.CalidadEscaneo
import com.enad.enadmovil.data.remote.CloudFunctionsApi.GrupoEscaneo
import com.enad.enadmovil.data.repository.ReportesRepository
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Con menos páginas que esto en algún lado de la comparación, todavía no se saca conclusión. */
private const val MIN_PAGINAS_COMPARACION = 5

// Claves del backend -> lo que ve el admin.
private val ETIQUETAS = mapOf(
    "ready" to "Listas (buena luz y quieto)", "not_ready" to "El resto",
    "dark" to "Oscuro", "low" to "Poca luz", "good" to "Buena luz", "glare" to "Mucho brillo", "unknown" to "Sin dato",
    "steady" to "Quieto", "moving" to "En movimiento",
    "on" to "Con linterna", "off" to "Sin linterna",
    "auto" to "Automática", "manual" to "Botón"
)

data class ReporteEscaneoUiState(
    val cargando: Boolean = true,
    val reporte: CalidadEscaneo? = null,
    val error: String? = null
)

/** BQ tipo 3 (#10): ¿las páginas escaneadas con buena luz y el celular quieto se leen mejor que las demás? */
class ReporteEscaneoViewModel(
    private val repo: ReportesRepository = ReportesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReporteEscaneoUiState())
    val uiState: StateFlow<ReporteEscaneoUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar(forzar: Boolean = false) {
        _uiState.value = _uiState.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            _uiState.value = try {
                ReporteEscaneoUiState(cargando = false, reporte = repo.calidadEscaneo(forzar))
            } catch (e: CloudFunctionsApi.ApiException) {
                val mensaje = when (e.status) {
                    403 -> "Solo un administrador puede ver este reporte."
                    404 -> "Este reporte todavía no está disponible en el servidor."
                    else -> e.message
                }
                ReporteEscaneoUiState(cargando = false, error = mensaje)
            } catch (e: Exception) {
                ReporteEscaneoUiState(cargando = false, error = "No se pudo cargar el reporte. Revisa tu conexión e intenta de nuevo.")
            }
        }
    }

    fun actualizar() = cargar(forzar = true)
}

@Composable
fun ReporteEscaneoSection(viewModel: ReporteEscaneoViewModel = viewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Calidad del escaneo",
            descripcion = "Filas que lee el escáner según la luz y el movimiento al tomar la foto (últimas 8 semanas).",
            cargando = estado.cargando,
            onActualizar = viewModel::actualizar
        )
        val carga: Carga<CalidadEscaneo?> = when {
            estado.cargando -> Carga.Cargando
            estado.error != null -> Carga.Fallo(estado.error.orEmpty())
            else -> Carga.Lista(estado.reporte)
        }
        ContenidoCarga(
            carga = carga,
            vacio = { it == null || it.paginas == 0 },
            textoVacio = "Todavía no hay páginas escaneadas en las últimas 8 semanas."
        ) { reporte ->
            if (reporte != null) {
                PanelComparacion(reporte)
                PanelAutomatica(reporte)
                PanelGrupos("Luz al tomar la foto", reporte.luz)
                PanelGrupos("Movimiento", reporte.quietud)
                PanelGrupos("Linterna", reporte.linterna)
                PanelGrupos("Disparo", reporte.disparo)
            }
        }
    }
}

/** La respuesta de la BQ: cuántas filas más leen las páginas listas y qué tanto fallan menos. */
@Composable
private fun PanelComparacion(reporte: CalidadEscaneo) {
    val listas = reporte.listas.find { it.clave == "ready" }
    val resto = reporte.listas.find { it.clave == "not_ready" }
    EnadPanel {
        Text("Listas vs. el resto", style = MaterialTheme.typography.titleMedium)
        Text(
            "${reporte.paginas} página(s) de ${reporte.docentes} docente(s)",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(conclusion(listas, resto), style = MaterialTheme.typography.labelLarge, color = EnadPrimary)
        listOfNotNull(listas, resto).forEach {
            Spacer(Modifier.height(8.dp))
            FilaGrupo(it)
        }
    }
}

private fun conclusion(listas: GrupoEscaneo?, resto: GrupoEscaneo?): String {
    val filasListas = listas?.filasPromedio
    val filasResto = resto?.filasPromedio
    if (listas == null || resto == null || filasListas == null || filasResto == null ||
        listas.paginas < MIN_PAGINAS_COMPARACION || resto.paginas < MIN_PAGINAS_COMPARACION
    ) {
        return "Aún no hay suficientes páginas para comparar (mínimo $MIN_PAGINAS_COMPARACION de cada lado)."
    }
    val diferencia = filasListas - filasResto
    return when {
        diferencia > 0 -> "Las páginas listas leen ${decimal(diferencia)} fila(s) más en promedio."
        diferencia < 0 -> "Las páginas listas leen ${decimal(-diferencia)} fila(s) menos en promedio."
        else -> "Las páginas listas y el resto leen lo mismo en promedio."
    }
}

@Composable
private fun PanelAutomatica(reporte: CalidadEscaneo) {
    EnadPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Disparo automático", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${reporte.docentesConAutomatica} de ${reporte.docentes} docente(s) lo usaron " +
                        "(${porcentajeTexto(reporte.porcentajeDocentesConAutomatica)})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                porcentajeTexto(reporte.porcentajePaginasAutomaticas),
                style = MaterialTheme.typography.headlineSmall,
                color = EnadPrimary
            )
        }
        Text("de las páginas", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.End))
    }
}

@Composable
private fun PanelGrupos(titulo: String, grupos: List<GrupoEscaneo>) {
    EnadPanel {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        // Los grupos sin páginas no aportan nada a la comparación
        grupos.filter { it.paginas > 0 }.forEach {
            Spacer(Modifier.height(8.dp))
            FilaGrupo(it)
        }
    }
}

@Composable
private fun FilaGrupo(grupo: GrupoEscaneo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnadBackground, ControlShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(ETIQUETAS[grupo.clave] ?: grupo.clave, style = MaterialTheme.typography.labelMedium)
            Text(
                "${grupo.paginas} página(s) · ${porcentajeTexto(grupo.porcentajeFallas)} sin leer",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            grupo.filasPromedio?.let { "${decimal(it)} filas" } ?: "—",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private fun decimal(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toLong().toString() else String.format(LOCALE_CO, "%.1f", valor)
