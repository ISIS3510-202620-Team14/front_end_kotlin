package com.enad.enadmovil.feature.horas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.repository.ContextoRepository
import com.enad.enadmovil.data.repository.HorasRepository
import com.enad.enadmovil.domain.model.DiaSesion
import com.enad.enadmovil.domain.model.OrigenValor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Lo que el docente está registrando para un día. */
data class RegistroDiaUi(
    val fecha: LocalDate,
    val planeadas: Double,
    val texto: String,
    val origen: String,          // suggested | manual
    val sugerido: Boolean,       // muestra el rótulo "Sugerido, ajústalo"
    val motivo: String = ""
) {
    val horas: Double? get() = texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..24.0 }
    val pideMotivo: Boolean get() = horas?.let { it < planeadas } ?: false
    val puedeGuardar: Boolean get() = horas != null && (!pideMotivo || motivo.isNotBlank())
}

data class HorasUiState(
    val dias: List<DiaSesion> = emptyList(),
    val registro: RegistroDiaUi? = null
)

class HorasViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = HorasRepository(app)
    private val contexto = ContextoRepository(app)
    private val registro = MutableStateFlow<RegistroDiaUi?>(null)

    val uiState: StateFlow<HorasUiState> = combine(repo.observarDias(), registro) { dias, actual ->
        HorasUiState(dias, actual)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HorasUiState())

    init {
        // Una lectura al abrir Horas: actualiza la última hora en la sede, que es el tope de la sugerencia.
        viewModelScope.launch { contexto.registrarPresencia() }
    }

    fun abrirDia(dia: DiaSesion) {
        viewModelScope.launch {
            val guardado = repo.reporteDelDia(dia.fecha)
            registro.value = if (guardado != null) {
                RegistroDiaUi(
                    fecha = dia.fecha,
                    planeadas = dia.horasProgramadas,
                    texto = formatearHoras(guardado.horasRealizadas),
                    origen = guardado.origen,
                    sugerido = guardado.origen == OrigenValor.SUGERIDO,
                    motivo = guardado.motivo.orEmpty()
                )
            } else {
                val sugerencia = repo.sugerencia(dia.fecha, dia.horasProgramadas)
                RegistroDiaUi(
                    fecha = dia.fecha,
                    planeadas = dia.horasProgramadas,
                    texto = sugerencia?.let { formatearHoras(it.horas) }.orEmpty(),
                    origen = if (sugerencia != null) OrigenValor.SUGERIDO else OrigenValor.MANUAL,
                    sugerido = sugerencia != null
                )
            }
        }
    }

    /** Si el docente toca el valor, deja de ser una sugerencia. */
    fun cambiarHoras(texto: String) =
        registro.update { it?.copy(texto = texto, origen = OrigenValor.MANUAL, sugerido = false) }

    fun cambiarMotivo(motivo: String) = registro.update { it?.copy(motivo = motivo) }

    fun cerrar() {
        registro.value = null
    }

    fun guardar() {
        val actual = registro.value ?: return
        val horas = actual.horas ?: return
        if (!actual.puedeGuardar) return
        viewModelScope.launch {
            repo.guardar(actual.fecha, actual.planeadas, horas, actual.origen, actual.motivo.takeIf { actual.pideMotivo })
            registro.value = null
        }
    }
}
