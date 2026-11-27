package com.enad.enadmovil.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.CloudFunctionsApi.FilaFallos
import com.enad.enadmovil.data.remote.CloudFunctionsApi.ReporteFallos
import com.enad.enadmovil.data.remote.CloudFunctionsApi.SemanaFallos
import com.enad.enadmovil.data.repository.ReportesRepository
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ReporteFallosUiState(
    val reporte: Carga<ReporteFallos> = Carga.Cargando
)

/** BQ tipo 1: sesiones de la app que terminan en un fallo y dónde se concentran. */
class ReporteFallosViewModel(
    private val repo: ReportesRepository = ReportesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReporteFallosUiState())
    val uiState: StateFlow<ReporteFallosUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar(forzar: Boolean = false) {
        _uiState.update { it.copy(reporte = Carga.Cargando) }
        viewModelScope.launch {
            val resultado = consultar { repo.fallos(forzar) }
            _uiState.update { it.copy(reporte = resultado) }
        }
    }

    fun actualizar() = cargar(forzar = true)

    private suspend fun <T> consultar(bloque: suspend () -> T): Carga<T> =
        try {
            Carga.Lista(bloque())
        } catch (e: CloudFunctionsApi.ApiException) {
            Carga.Fallo(mensajeError(e))
        } catch (e: IOException) {
            Carga.Fallo("No se pudo cargar. Revisa tu conexión e intenta de nuevo.")
        } catch (e: JSONException) {
            Carga.Fallo("No se pudo leer la respuesta. Revisa tu conexión e intenta de nuevo.")
        }
}

private val FORMATO_SEMANA_CORTA = DateTimeFormatter.ofPattern("d MMM", LOCALE_CO)

@Composable
fun ReporteFallosSection(viewModel: ReporteFallosViewModel = viewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Fallos de la app",
            descripcion = "Sesiones que terminan con la app cerrada por un error (últimas 8 semanas).",
            cargando = estado.reporte is Carga.Cargando,
            onActualizar = viewModel::actualizar
        )
        ContenidoCarga(estado.reporte, vacio = { it.sesiones == 0 }, textoVacio = "Todavía no hay sesiones registradas.") { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cifra("Sesiones sin fallos", porcentajeTexto(r.porcentajeSinFallos), "${r.sesiones} sesiones", Modifier.weight(1f))
                Cifra("Fallos", r.fallos.toString(), "${porcentajeTexto(r.porcentajeFallos)} de las sesiones", Modifier.weight(1f))
            }
            EnadPanel {
                Text("Fallos por semana", style = MaterialTheme.typography.titleMedium)
                Text("% de sesiones que terminaron en un fallo", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                LineaSemanas(r.porSemana)
            }
            GrupoFallos("Pantalla donde pasó", r.porPantalla, mostrarSesiones = false)
            GrupoFallos("Versión de la app", r.porVersion)
            GrupoFallos("Modelo del celular", r.porModelo)
            GrupoFallos("Versión de Android", r.porAndroid.map { it.copy(etiqueta = "API ${it.etiqueta}") })
        }
    }
}

@Composable
private fun Cifra(etiqueta: String, valor: String, detalle: String, modifier: Modifier) {
    EnadPanel(modifier = modifier) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.headlineSmall, color = EnadPrimary, maxLines = 1)
        Text(detalle, style = MaterialTheme.typography.bodySmall)
    }
}

/** Línea con el % de fallos de cada semana; las semanas sin sesiones quedan sin punto. */
@Composable
private fun LineaSemanas(semanas: List<SemanaFallos>) {
    val maximo = (semanas.mapNotNull { it.porcentaje }.maxOrNull() ?: 0.0).coerceAtLeast(5.0)
    Row(Modifier.fillMaxWidth().height(140.dp)) {
        Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(porcentajeTexto(Math.round(maximo * 10) / 10.0), style = MaterialTheme.typography.labelSmall)
            Text("0 %", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.padding(start = 8.dp))
        Canvas(Modifier.weight(1f).fillMaxHeight()) {
            drawLine(EnadLine, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            if (semanas.isEmpty()) return@Canvas
            val paso = if (semanas.size > 1) size.width / (semanas.size - 1) else 0f
            val puntos = semanas.mapIndexedNotNull { i, s ->
                s.porcentaje?.let { Offset(i * paso, size.height - (it / maximo * size.height).toFloat()) }
            }
            if (puntos.size > 1) {
                val linea = Path().apply {
                    moveTo(puntos.first().x, puntos.first().y)
                    puntos.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(linea, EnadPrimary, style = Stroke(width = 5f))
            }
            puntos.forEach { drawCircle(EnadPrimary, radius = 8f, center = it) }
        }
    }
    Row(Modifier.fillMaxWidth().padding(start = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        listOfNotNull(semanas.firstOrNull(), semanas.lastOrNull().takeIf { semanas.size > 1 }).forEach {
            Text(runCatching { LocalDate.parse(it.semana).format(FORMATO_SEMANA_CORTA) }.getOrDefault(it.semana), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Barras de un desglose. En pantalla la barra es la parte de los fallos, en lo demás el % de sesiones con fallo. */
@Composable
private fun GrupoFallos(titulo: String, filas: List<FilaFallos>, mostrarSesiones: Boolean = true) {
    if (filas.isEmpty()) return
    val maximo = filas.mapNotNull { it.porcentaje }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    EnadPanel {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        filas.take(6).forEach { f ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(f.etiqueta, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(
                        if (mostrarSesiones) "${f.fallos} de ${f.sesiones} · ${porcentajeTexto(f.porcentaje)}" else "${f.fallos} · ${porcentajeTexto(f.porcentaje)}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).background(EnadLine, RoundedCornerShape(50))) {
                    val fraccion = ((f.porcentaje ?: 0.0) / maximo).toFloat().coerceIn(0f, 1f)
                    if (fraccion > 0f) Box(Modifier.fillMaxWidth(fraccion).height(8.dp).background(EnadPrimary, RoundedCornerShape(50)))
                }
            }
        }
    }
}
