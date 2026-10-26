package com.enad.enadmovil.feature.ayuda

import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

internal fun claveObjetivo(flujo: Guia, paso: Int) = "${flujo.name}:$paso"

class GuiaViewModel : ViewModel() {
    var flujo by mutableStateOf<Guia?>(null)
        private set
    var paso by mutableIntStateOf(-1)
        private set
    var requisito by mutableStateOf<String?>(null)
        private set

    var horasPideMotivo = false
    private val objetivos = mutableStateMapOf<String, Rect>()

    val activa: Boolean get() = flujo != null && paso >= 0
    private val pasos: List<PasoGuia> get() = flujo?.let {
        PASOS_GUIA.getValue(it)
    }.orEmpty()
    val actual: PasoGuia? get() = if (activa) pasos.getOrNull(paso)  else null
    val completa: Boolean get() = activa && paso == pasos.lastIndex
    val mostrarContinuar: Boolean get() = actual?.continuar == true && !completa
    val puedeContinuar: Boolean get() = mostrarContinuar && (actual?.opcional == true || requisito == null)
    val puedeVolver: Boolean get() = flujo == Guia.ASISTENCIA && paso > 1 && !completa
    val objetivo: Rect? get() = flujo?.let {
        objetivos[claveObjetivo(it, paso)]}

    fun iniciar(guia: Guia) {
        flujo = guia
        paso = 0
        requisito = null
    }

    fun avanzar(guia: Guia, esperado: Int) {
        if (flujo != guia || paso != esperado) return
        paso++
        requisito = null
    }

    fun siguiente() {
        if (!puedeContinuar) return
        paso++
        if (flujo == Guia.HORAS && paso == 4 && !horasPideMotivo) paso++
        requisito = null
    }

    fun volver() {
        if(!puedeVolver) return
        paso --
        requisito = null
    }
    fun salir() {
        flujo = null
        paso = -1
        requisito = null
    }

    fun fijarRequisito(texto: String?) {
        if (requisito != texto ) requisito = texto
    }

    internal fun registrarObjetivo(clave: String, rect: Rect) {
        if(objetivos[clave] != rect) objetivos[clave] = rect
    }

    internal fun quitarObjetivo(clave: String) {
        objetivos.remove(clave)
    }
}