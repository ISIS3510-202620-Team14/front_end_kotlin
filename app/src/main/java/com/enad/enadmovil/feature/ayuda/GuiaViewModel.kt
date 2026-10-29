package com.enad.enadmovil.feature.ayuda

import android.app.Application
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.enad.enadmovil.data.remote.FirebaseModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

internal fun claveObjetivo(flujo: Guia, paso: Int) = "${flujo.name}:$paso"

class GuiaViewModel(app: Application) : AndroidViewModel(app) {
    var flujo by mutableStateOf<Guia?>(null)
        private set
    var paso by mutableIntStateOf(-1)
        private set
    var requisito by mutableStateOf<String?>(null)
        private set

    var horasPideMotivo = false

    /** Zona de la pantalla donde se ve la app (sin la franja de la tarjeta). El recuadro no sale de ella. */
    var area by mutableStateOf<Rect?>(null)
        private set

    /** Alto real de la tarjeta de la guía, en píxeles: es lo que se reserva debajo de la app. */
    var alturaTarjeta by mutableIntStateOf(0)
        private set
    private var corrida = ""
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
        corrida = UUID.randomUUID().toString()
        registrar(GuiaEventos.INICIADA)
        uid()?.let { UsoGuias.registrarInicio(getApplication(), it, guia) }
    }

    fun avanzar(guia: Guia, esperado: Int) {
        if (flujo != guia || paso != esperado) return
        registrar(GuiaEventos.PASO_COMPLETADO)
        paso++
        requisito = null
        if (completa) {
            registrar(GuiaEventos.COMPLETADA)
            uid()?.let { UsoGuias.registrarFin(getApplication(), it, guia) }
        }
    }

    fun siguiente() {
        if (!puedeContinuar) return
        registrar(if (actual?.opcional == true) GuiaEventos.PASO_OMITIDO else GuiaEventos.PASO_COMPLETADO)
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
        if (activa && !completa) registrar(GuiaEventos.ABANDONADA)
        flujo = null
        paso = -1
        requisito = null
    }

    fun fijarRequisito(texto: String?) {
        if (requisito != texto ) requisito = texto
    }

    /** Uso de las guías de esta cuenta en este celular. Se lee fuera del hilo principal. */
    suspend fun resumenUso(): ResumenUso? {
        val uid = uid() ?: return null
        return withContext(Dispatchers.IO) { UsoGuias.leer(getApplication(), uid) }
    }

    private fun uid(): String? = FirebaseModule.auth.currentUser?.uid

    private fun registrar(nombre: String) {
        flujo?.let {
            GuiaEventos.registrar(nombre, it, corrida, paso)
        }
    }
    fun fijarArea(rect: Rect) {
        if (area != rect) area = rect
    }

    fun fijarAlturaTarjeta(px: Int) {
        if (alturaTarjeta != px) alturaTarjeta = px
    }

    internal fun registrarObjetivo(clave: String, rect: Rect) {
        if(objetivos[clave] != rect) objetivos[clave] = rect
    }

    internal fun quitarObjetivo(clave: String) {
        objetivos.remove(clave)
    }
}