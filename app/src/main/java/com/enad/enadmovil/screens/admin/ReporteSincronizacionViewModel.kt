package com.enad.enadmovil.ui.screens.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.local.ArchivoRespuestas
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.repository.ReportesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReporteUiState(
    val cargando: Boolean = true,
    val reporte: CloudFunctionsApi.ReporteSincronizacion? = null,
    val error: String? = null,
    val guardadoEn: Long? = null
)

/** BQ tipo 1: carga el reporte semanal de sesiones de agrupación que no se sincronizaron en 24 h. */
class ReporteSincronizacionViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ReportesRepository(archivo = ArchivoRespuestas(application))
    private val alVolverRed = AlVolverLaRed(application) { cargar(forzar = true) }

    private val _uiState = MutableStateFlow(ReporteUiState())
    val uiState: StateFlow<ReporteUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar(forzar: Boolean = false) {
        _uiState.value = ReporteUiState(cargando = true)
        viewModelScope.launch {
            _uiState.value = try {
                val lectura = repo.sincronizacionAgrupaciones(forzar)
                if (lectura.guardadoEn != null) alVolverRed.esperando = true
                ReporteUiState(cargando = false, reporte = lectura.datos, guardadoEn = lectura.guardadoEn)
            } catch (e: CloudFunctionsApi.ApiException) {
                alVolverRed.esperando = true
                val mensaje = if (e.status == 403) "Solo un administrador puede ver este reporte." else e.message
                ReporteUiState(cargando = false, error = mensaje)
            } catch (e: Exception) {
                alVolverRed.esperando = true
                ReporteUiState(cargando = false, error = "No se pudo cargar el reporte. Revisa tu conexión e intenta de nuevo.")
            }
        }
    }
    fun actualizar() = cargar(forzar = true)

    override fun onCleared() {
        alVolverRed.cerrar()
    }
}
