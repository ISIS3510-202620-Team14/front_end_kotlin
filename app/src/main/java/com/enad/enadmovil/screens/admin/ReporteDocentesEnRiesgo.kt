package com.enad.enadmovil.ui.screens.admin

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.local.ArchivoRespuestas
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.repository.ReportesRepository
import com.enad.enadmovil.domain.model.DocentesEnRiesgo
import com.enad.enadmovil.screens.teacher.AvisoDatosGuardados
import com.enad.enadmovil.ui.components.EnadPanel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException

data class ReporteDocentesEnRiesgoUiState(
    val reporte: Carga<DocentesEnRiesgo> = Carga.Cargando,
    val guardadoEn: Long? = null
)

class ReporteDocentesEnRiesgoViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ReportesRepository(archivo = ArchivoRespuestas(application))

    private val _uiState = MutableStateFlow(ReporteDocentesEnRiesgoUiState())
    val uiState: StateFlow<ReporteDocentesEnRiesgoUiState> = _uiState.asStateFlow()
    private val alVolverRed = AlVolverLaRed(application) { cargar(forzar = true) }

    init {
        cargar()
    }

    fun cargar(forzar: Boolean = false) {
        _uiState.update { it.copy(reporte = Carga.Cargando) }
        viewModelScope.launch {
            var guardadoEn: Long? = null
            val resultado: Carga<DocentesEnRiesgo> = try {
                val lectura = repo.docentesEnRiesgo(forzar)
                guardadoEn = lectura.guardadoEn
                Carga.Lista(lectura.datos)
            } catch (e: CloudFunctionsApi.ApiException) {
                Carga.Fallo(mensajeError(e))
            } catch (e: IOException) {
                Carga.Fallo("No se pudo cargar. Revisa tu conexión e intenta de nuevo.")
            } catch (e: JSONException) {
                Carga.Fallo("No se pudo leer la respuesta. Revisa tu conexión e intenta de nuevo.")
            }
            if (resultado is Carga.Fallo || guardadoEn != null) alVolverRed.esperando = true
            _uiState.update { it.copy(reporte = resultado, guardadoEn = guardadoEn) }
        }
    }

    fun actualizar() = cargar(forzar = true)

    override fun onCleared() {
        alVolverRed.cerrar()
    }
}

@Composable
fun ReporteDocentesEnRiesgoSection(viewModel: ReporteDocentesEnRiesgoViewModel = viewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val semana = estado.reporte.datos?.let { semanaLegible(it.ultimaSemana).lowercase(LOCALE_CO) } ?: "última semana completa"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Docentes en riesgo",
            descripcion = "Docentes que pueden dejar de usar la app porque sus aperturas semanales bajaron.",
            cargando = estado.reporte is Carga.Cargando,
            onActualizar = viewModel::actualizar
        )
        estado.guardadoEn?.let {
            if (estado.reporte is Carga.Lista) AvisoDatosGuardados(it, onActualizar = viewModel::actualizar, titulo = "Reporte guardado")
        }
        ContenidoCarga(
            estado.reporte,
            vacio = { it.docentes.isEmpty() },
            textoVacio = "Ningún docente bajó su uso en la $semana. No hay a quién contactar."
        ) { r ->
            Text(
                if (r.docentes.size == 1) "1 docente abrió la app la mitad o menos que antes en la $semana."
                else "${r.docentes.size} docentes abrieron la app la mitad o menos que antes en la $semana.",
                style = MaterialTheme.typography.bodyMedium
            )
            r.docentes.forEach { d ->
                EnadPanel {
                    Text(d.nombre ?: "Docente sin nombre", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "La semana pasada abrió la app ${veces(d.semanaPasada.toDouble())}; " +
                            "antes lo hacía unas ${veces(d.semanaHabitual)} por semana.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun veces(valor: Double): String {
    val numero = if (valor % 1.0 == 0.0) valor.toLong().toString() else String.format(LOCALE_CO, "%.1f", valor)
    return if (valor == 1.0) "$numero vez" else "$numero veces"
}