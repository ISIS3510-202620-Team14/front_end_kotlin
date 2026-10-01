package com.enad.enadmovil.ui.screens.teacher.scan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.ocr.TextRecognizerDataSource
import com.enad.enadmovil.domain.parser.ListaAsistenciaParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val ocr = TextRecognizerDataSource(app)
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onFotoCapturada(uri: Uri) {
        _uiState.value = ScanUiState(procesando = true)
        viewModelScope.launch {
            runCatching { ListaAsistenciaParser.parsear(ocr.reconocer(uri)) }
                .onSuccess { filas ->
                    _uiState.update {
                        it.copy(
                            procesando = false,
                            filas = filas,
                            error = if (filas.isEmpty()) {
                                "No se detectó ninguna fila. Intenta con más luz y la lista de frente."
                            } else null
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(procesando = false, error = "No se pudo leer la foto.") }
                }
        }
    }

    fun limpiar() {
        _uiState.value = ScanUiState()
    }
}