package com.enad.enadmovil.ui.screens.teacher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.repository.ContextoRepository
import com.enad.enadmovil.domain.model.GrupoDelDia
import com.enad.enadmovil.domain.model.MomentoDelDia
import com.enad.enadmovil.domain.model.ResumenDelDia
import com.enad.enadmovil.domain.model.OrigenValor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

data class ContextoHoyUiState(
    val cargando: Boolean = false,
    val confirmado: GrupoDelDia? = null,
    val sugerencia: GrupoDelDia? = null,
    val grupos: List<GrupoDelDia> = emptyList(),
    val eligiendo: Boolean = false,
    // Tarjeta del momento del día: qué le toca hacer ahora al docente
    val resumen: ResumenDelDia? = null,
    val momento: MomentoDelDia? = null
)

/** "Hoy": sugiere el grupo del día según la institución donde está el docente. Nunca decide por él. */
class ContextoHoyViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ContextoRepository(app)
    private val _uiState = MutableStateFlow(ContextoHoyUiState())
    val uiState: StateFlow<ContextoHoyUiState> = _uiState.asStateFlow()
    private var iniciado = false

    fun tienePermisoUbicacion(): Boolean = repo.tienePermisoUbicacion()

    /** La pantalla lo llama una vez, después de pedir (o no) el permiso de ubicación. */
    fun iniciar() {
        if (iniciado) return
        iniciado = true
        _uiState.update { it.copy(cargando = true) }
        viewModelScope.launch {
            repo.descargarCatalogo()
            val schoolId = repo.registrarPresencia()
            val confirmado = repo.contextoDeHoy()
            val sugerencia = if (confirmado == null) repo.sugerir(schoolId) else null
            val grupos = repo.gruposDelDocente()
            _uiState.value = ContextoHoyUiState(
                confirmado = confirmado,
                sugerencia = sugerencia,
                grupos = grupos,
                eligiendo = confirmado == null && sugerencia == null && grupos.isNotEmpty()
            )
            refrescarMomento()
        }
    }

    /**
     * Vuelve a calcular el momento del día. La pantalla lo llama cada vez que Hoy vuelve a verse,
     * porque al regresar de Asistencia u Horas lo hecho cambia, y con la hora también cambia el momento.
     */
    fun refrescarMomento() {
        viewModelScope.launch {
            val estado = _uiState.value
            val resumen = repo.resumenDelDia(estado.confirmado ?: estado.sugerencia) ?: return@launch
            _uiState.update { it.copy(resumen = resumen, momento = MomentoDelDia.de(resumen, LocalTime.now())) }
        }
    }

    fun aceptarSugerencia() {
        val grupo = _uiState.value.sugerencia ?: return
        confirmar(grupo, OrigenValor.SUGERIDO)
    }

    fun cambiar() = _uiState.update { it.copy(eligiendo = true) }

    fun elegir(grupo: GrupoDelDia) = confirmar(grupo, OrigenValor.MANUAL)

    private fun confirmar(grupo: GrupoDelDia, origen: String) {
        viewModelScope.launch {
            repo.confirmar(grupo.id, origen)
            _uiState.update { it.copy(confirmado = grupo, sugerencia = null, eligiendo = false) }
            refrescarMomento()
        }
    }
}
