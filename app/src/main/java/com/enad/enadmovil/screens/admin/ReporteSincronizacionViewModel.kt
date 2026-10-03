package com.enad.enadmovil.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.repository.ReportesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReporteUiState(
    val cargando: Boolean = true,
    val reporte: CloudFunctionsApi.ReporteSincronizacion? = null,
    val error: String? = null
)

/** BQ tipo 1: carga el reporte semanal de sesiones de agrupación que no se sincronizaron en 24 h. */
class ReporteSincronizacionViewModel(
    private val repo: ReportesRepository = ReportesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReporteUiState())
    val uiState: StateFlow<ReporteUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _uiState.value = ReporteUiState(cargando = true)
        viewModelScope.launch {
            _uiState.value = try {
                ReporteUiState(cargando = false, reporte = repo.sincronizacionAgrupaciones())
            } catch (e: CloudFunctionsApi.ApiException) {
                val mensaje = if (e.status == 403) "Solo un administrador puede ver este reporte." else e.message
                ReporteUiState(cargando = false, error = mensaje)
            } catch (e: Exception) {
                ReporteUiState(cargando = false, error = "No se pudo cargar el reporte. Revisa tu conexión e intenta de nuevo.")
            }
        }
    }
}
