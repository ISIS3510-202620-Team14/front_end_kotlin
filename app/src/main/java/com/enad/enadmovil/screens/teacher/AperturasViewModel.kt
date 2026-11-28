package com.enad.enadmovil.ui.screens.teacher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.repository.AperturasRepository
import com.enad.enadmovil.data.telemetria.EventosReenganche
import com.enad.enadmovil.domain.model.RiesgoAbandono
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AperturasUiState(
    val aperturas: Int = 0,
    val meta: Int = 2,
    val riesgo: RiesgoAbandono = RiesgoAbandono.SIN_RIESGO,
    val riesgoDescartado: Boolean = false,
    val pendientes: Int = 0,
    val cargado: Boolean = false
) {
    val mensaje: String get() = when {
        aperturas >= meta -> "Meta cumplida. Gracias por mantener al día a tus estudiantes."
        meta - aperturas == 1 -> "Vuelve a abrir la app una vez más esta semana para seguir el avance de tus estudiantes."
        else -> "Abre la app ${meta - aperturas} veces esta semana para seguir el avance de tus estudiantes."
    }
    val mostrarRiesgo: Boolean get() = riesgo.enRiesgo && !riesgoDescartado
}

class AperturasViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AperturasRepository(app)
    private val riesgo = MutableStateFlow(RiesgoAbandono.SIN_RIESGO)
    private val riesgoDescartado = MutableStateFlow(false)

    val uiState: StateFlow<AperturasUiState> =
        combine(repo.observarSemana(), repo.observarPendientes(), riesgo, riesgoDescartado) { aperturas, pendientes, nivel, descartado ->
            AperturasUiState(aperturas = aperturas, riesgo = nivel, riesgoDescartado = descartado, pendientes = pendientes, cargado = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AperturasUiState())

    init {
        viewModelScope.launch {
            val calculado = RiesgoAbandono.desdeAperturas(
                repo.semanasCompletas(RiesgoAbandono.SEMANAS_BASE + 1)
            )
            riesgo.value = calculado
            if (calculado.enRiesgo) EventosReenganche.registrar(EventosReenganche.MOSTRADA, calculado)
        }
    }

    fun aceptarRiesgo() {
        EventosReenganche.registrar(EventosReenganche.USADA, riesgo.value)
        descartarRiesgo()
    }

    fun descartarRiesgo() {
        riesgoDescartado.value = true
    }

    fun enviarPendientes() {
        viewModelScope.launch { runCatching { repo.sincronizarPendientes() } }
    }
}