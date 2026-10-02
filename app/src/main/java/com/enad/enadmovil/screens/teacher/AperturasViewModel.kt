package com.enad.enadmovil.ui.screens.teacher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.repository.AperturasRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AperturasUiState(val aperturas: Int = 0, val meta: Int = 2) {
    val mostrarRecordatorio: Boolean get() = aperturas < meta
}

class AperturasViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AperturasRepository(app)

    val uiState: StateFlow<AperturasUiState> = repo.observarSemana()
        .map { AperturasUiState(aperturas = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AperturasUiState())
}