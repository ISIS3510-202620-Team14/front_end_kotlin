package com.enad.enadmovil.ui.screens.admin

import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.local.ArchivoRespuestas
import com.enad.enadmovil.data.local.ReporteGuardado
import com.enad.enadmovil.data.local.ReportesAsistencia
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.repository.AuthRepository
import com.enad.enadmovil.data.repository.MapaAsistenciaRepository
import com.enad.enadmovil.data.repository.MapaAsistenciaRepository.Origen
import com.enad.enadmovil.domain.model.MapaAsistencia
import com.enad.enadmovil.screens.teacher.AvisoDatosGuardados
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadNoAsistioBg
import com.enad.enadmovil.ui.theme.EnadPendienteBg
import com.enad.enadmovil.ui.theme.EnadPillBg
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSuccessText
import com.enad.enadmovil.ui.theme.EnadSurface
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MapaAsistenciaUiState(
    val mapa: MapaAsistencia? = null,
    val origen: Origen? = null,
    val actualizando: Boolean = true,
    val error: String? = null,
    val reportes: List<ReporteGuardado> = emptyList(),
    val abierto: ReporteAbierto? = null,
    val aviso: String? = null
)

data class ReporteAbierto(val reporte: ReporteGuardado, val texto: String)

/** Mapa de asistencia: cache then network y recarga sola cuando vuelve el internet. */
class MapaAsistenciaViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MapaAsistenciaRepository(ArchivoRespuestas(application))
    private val _uiState = MutableStateFlow(MapaAsistenciaUiState())
    val uiState: StateFlow<MapaAsistenciaUiState> = _uiState.asStateFlow()
    private var carga: Job? = null
    private val archivos = ReportesAsistencia(application)
    private val uid get() = AuthRepository().uidActual() ?: "admin"

    private val conectividad = application.getSystemService(ConnectivityManager::class.java)
    private val alVolverRed = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (_uiState.value.origen == Origen.DISCO) cargar()
        }
    }

    init {
        cargar()
        cargarReportes()
        runCatching { conectividad?.registerDefaultNetworkCallback(alVolverRed) }
    }

    fun cargar() {
        carga?.cancel()
        _uiState.update { it.copy(actualizando = true, error = null) }
        carga = viewModelScope.launch {
            repo.cargar()
                .catch { e ->
                    val mensaje = if (e is CloudFunctionsApi.ApiException) mensajeError(e) else "Aún no hay ninguna asistencia cargada."
                    _uiState.update { it.copy(actualizando = false, error = if (it.mapa == null) mensaje else null) }
                }
                .collect { r ->
                    _uiState.update { it.copy(mapa = r.mapa, origen = r.origen, actualizando = r.origen == Origen.CACHE) }
                }
        }
    }

    private fun cargarReportes() {
        viewModelScope.launch {
            val reportes = runCatching { archivos.listar(uid) }.getOrDefault(emptyList())
            _uiState.update { it.copy(reportes = reportes) }
        }
    }

    fun guardarReporte() {
        val mapa = _uiState.value.mapa ?: return
        viewModelScope.launch {
            val aviso = if (runCatching { archivos.guardar(uid, mapa) }.isSuccess) "Reporte guardado" else "No se pudo guardar el reporte"
            _uiState.update { it.copy(aviso = aviso) }
            cargarReportes()
        }
    }

    fun abrirReporte(reporte: ReporteGuardado) {
        viewModelScope.launch {
            val texto = runCatching { archivos.leer(reporte) }.getOrNull() ?: return@launch
            _uiState.update { it.copy(abierto = ReporteAbierto(reporte, texto)) }
        }
    }

    fun cerrarReporte() = _uiState.update { it.copy(abierto = null) }

    fun borrarReporte(reporte: ReporteGuardado) {
        _uiState.update { it.copy(abierto = null) }
        viewModelScope.launch {
            runCatching { archivos.borrar(reporte) }
            cargarReportes()
        }
    }

    fun avisoMostrado() = _uiState.update { it.copy(aviso = null) }

    override fun onCleared() {
        runCatching { conectividad?.unregisterNetworkCallback(alVolverRed) }
    }
}

private val DIAS_SEMANA = listOf("Lun", "Mar", "Mié", "Jue", "Vie")
private val ETIQUETA_REPORTE = DateTimeFormatter.ofPattern("'Reporte del' dd/MM/yyyy '·' HH:mm").withZone(ZoneId.of("America/Bogota"))

internal fun colorAsistencia(porcentaje: Double?): Color = when {
    porcentaje == null -> EnadLine
    porcentaje >= 85 -> EnadPillBg
    porcentaje >= 70 -> EnadPendienteBg
    else -> EnadNoAsistioBg
}

internal fun fraseCaida(mapa: MapaAsistencia): String {
    val caida = mapa.caidaMayor ?: return "Ninguna institución bajó esta semana"
    return "${caida.nombre} bajó ${Math.round(Math.abs(caida.cambio ?: 0.0))} puntos"
}

@Composable
fun ReporteMapaAsistenciaSection(viewModel: MapaAsistenciaViewModel = viewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val mapa = estado.mapa
    val context = LocalContext.current

    LaunchedEffect(estado.aviso) {
        estado.aviso?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.avisoMostrado()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Mapa de asistencia",
            descripcion = "Asistencia de cada institución de lunes a viernes.",
            cargando = estado.actualizando,
            onActualizar = viewModel::cargar
        )
        when {
            mapa != null -> {
                EstadoConexion(estado, mapa, viewModel::cargar)
                EnadPanel {
                    Text(semanaLegible(mapa.dias.first()), style = MaterialTheme.typography.bodyMedium)
                    Text(porcentajeTexto(mapa.semana), style = MaterialTheme.typography.headlineLarge, color = EnadPrimary)
                    Text(fraseCaida(mapa), style = MaterialTheme.typography.bodyMedium)
                }
                TablaAsistencia(mapa)
            }
            estado.error != null -> MensajePanel(estado.error!!)
            else -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EnadInk)
            }
        }
        PanelReportes(estado.reportes, puedeGuardar = mapa != null, onGuardar = viewModel::guardarReporte, onAbrir = viewModel::abrirReporte)
    }

    estado.abierto?.let { abierto ->
        HojaReporte(
            abierto = abierto,
            onCerrar = viewModel::cerrarReporte,
            onBorrar = { viewModel.borrarReporte(abierto.reporte) },
            onExportar = {
                val envio = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte de asistencia")
                    putExtra(Intent.EXTRA_TEXT, abierto.texto)
                }
                context.startActivity(Intent.createChooser(envio, "Exportar reporte"))
            }
        )
    }
}

@Composable
private fun PanelReportes(reportes: List<ReporteGuardado>, puedeGuardar: Boolean, onGuardar: () -> Unit, onAbrir: (ReporteGuardado) -> Unit) {
    EnadPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reportes guardados", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onGuardar, enabled = puedeGuardar) {
                Icon(Icons.Outlined.SaveAlt, contentDescription = null, tint = EnadPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Guardar", color = EnadPrimary)
            }
        }
        Text("Se guardan en este celular. Puedes abrirlos sin internet.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        if (reportes.isEmpty()) {
            Text("Todavía no has guardado reportes.", style = MaterialTheme.typography.bodyMedium)
        }
        reportes.forEach { reporte ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onAbrir(reporte) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = EnadSoftInk)
                Column(Modifier.weight(1f)) {
                    Text(ETIQUETA_REPORTE.format(Instant.ofEpochMilli(reporte.guardadoEn)), style = MaterialTheme.typography.bodyLarge)
                    Text(String.format(LOCALE_CO, "%.1f KB", reporte.bytes / 1024.0), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = EnadSoftInk)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaReporte(abierto: ReporteAbierto, onCerrar: () -> Unit, onBorrar: () -> Unit, onExportar: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onCerrar, containerColor = EnadSurface) {
        Column(Modifier.fillMaxHeight(0.8f).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            SelectionContainer(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(abierto.texto, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBorrar, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = EnadInk)
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar", color = EnadInk)
                }
                Button(onClick = onExportar, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = EnadPrimary)) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar")
                }
            }
        }
    }
}

@Composable
private fun EstadoConexion(estado: MapaAsistenciaUiState, mapa: MapaAsistencia, onActualizar: () -> Unit) {
    when {
        estado.origen == Origen.DISCO -> AvisoDatosGuardados(mapa.guardadoEn, onActualizar = onActualizar, titulo = "Mapa guardado")
        estado.actualizando -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(color = EnadInk, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            Text("Actualizando…", style = MaterialTheme.typography.bodySmall)
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.CloudDone, contentDescription = null, tint = EnadSuccessText, modifier = Modifier.size(18.dp))
            Text("Conectado", style = MaterialTheme.typography.bodySmall, color = EnadSuccessText)
        }
    }
}

@Composable
private fun TablaAsistencia(mapa: MapaAsistencia) {
    EnadPanel {
        FilaTabla("") {
            DIAS_SEMANA.forEach { Text(it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f).padding(vertical = 4.dp), maxLines = 1) }
        }
        mapa.instituciones.forEach { institucion ->
            FilaTabla(institucion.nombre) {
                institucion.dias.forEach { CeldaAsistencia(it) }
            }
        }
    }
}

@Composable
private fun FilaTabla(nombre: String, celdas: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(3f))
        celdas()
    }
}

@Composable
private fun RowScope.CeldaAsistencia(porcentaje: Double?) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .padding(2.dp)
            .background(colorAsistencia(porcentaje), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(porcentaje?.let { Math.round(it).toString() } ?: "—", style = MaterialTheme.typography.bodyMedium, color = EnadInk)
    }
}
