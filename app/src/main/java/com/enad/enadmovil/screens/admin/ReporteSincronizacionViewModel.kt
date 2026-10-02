package com.enad.enadmovil.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.repository.AuthRepository
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
    private val auth: AuthRepository = AuthRepository()
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
                val token = auth.obtenerToken()
                if (token == null) {
                    ReporteUiState(cargando = false, error = "Inicia sesión de nuevo para ver el reporte.")
                } else {
                    ReporteUiState(cargando = false, reporte = CloudFunctionsApi.reporteSesionesAgrupacion(token))
                }
            } catch (e: CloudFunctionsApi.ApiException) {
                val mensaje = if (e.status == 403) "Solo un administrador puede ver este reporte." else e.message
                ReporteUiState(cargando = false, error = mensaje)
            } catch (e: Exception) {
                ReporteUiState(cargando = false, error = "No se pudo cargar el reporte. Revisa tu conexión e intenta de nuevo.")
            }
        }
    }
}
