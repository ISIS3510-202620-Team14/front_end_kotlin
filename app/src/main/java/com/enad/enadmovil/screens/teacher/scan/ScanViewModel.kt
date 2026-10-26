package com.enad.enadmovil.ui.screens.teacher.scan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.ocr.HilosOcr
import com.enad.enadmovil.data.ocr.TextRecognizerDataSource
import com.enad.enadmovil.domain.parser.ListaAsistenciaParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val hilos = HilosOcr()
    private val ocr = TextRecognizerDataSource(app, hilos.dispatcher.executor)
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()
    private var siguientePagina = 1

    fun onFotoCapturada(uri: Uri) {
        val numero = siguientePagina++
        _uiState.update {
            it.copy(procesando = true, error = null, paginas = it.paginas + PaginaEscaneada(numero, EstadoPagina.Leyendo))
        }
        viewModelScope.launch {
            // El OCR y el armado de filas corren en los hilos del escáner; aquí solo vuelve el resultado
            runCatching { withContext(hilos.dispatcher) { ListaAsistenciaParser.parsear(ocr.reconocer(uri)) } }
                .onSuccess { filas ->
                    _uiState.update {
                        it.copy(
                            procesando = false,
                            filas = it.filas + filas,
                            paginas = it.paginas.marcar(numero, EstadoPagina.Lista(filas.size)),
                            error = if (filas.isEmpty()) {
                                "No se detectó ninguna fila. Intenta con más luz y la lista de frente."
                            } else null
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            procesando = false,
                            paginas = it.paginas.marcar(numero, EstadoPagina.Fallo),
                            error = "No se pudo leer la foto."
                        )
                    }
                }
        }
    }

    /** El sheet ya copió las filas nuevas a su lista: se vacían para no sumarlas dos veces. */
    fun tomarFilas() {
        _uiState.update { it.copy(filas = emptyList()) }
    }

    fun limpiar() {
        siguientePagina = 1
        _uiState.value = ScanUiState()
    }

    private fun List<PaginaEscaneada>.marcar(numero: Int, estado: EstadoPagina) =
        map { if (it.numero == numero) it.copy(estado = estado) else it }
}
