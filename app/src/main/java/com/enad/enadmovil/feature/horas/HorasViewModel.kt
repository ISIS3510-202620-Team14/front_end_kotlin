package com.enad.enadmovil.feature.horas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.repository.ContextoRepository
import com.enad.enadmovil.data.repository.HorasRepository
import com.enad.enadmovil.domain.model.DiaSesion
import com.enad.enadmovil.domain.model.OrigenValor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Motivos de horas no realizadas, los mismos de Flutter. Se guardan como "Categoría: detalle". */
val MOTIVOS_HORAS = listOf("Familiar", "Personal", "Salud", "Transporte", "Institucional", "Otro")

private fun leerHoras(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..24.0 }

/** Lo que el docente está registrando para un día. */
data class RegistroDiaUi(
    val fecha: LocalDate,
    val textoPlaneadas: String,
    val texto: String,
    val origen: String,          // suggested | manual
    val sugerido: Boolean,       // muestra el rótulo "Sugerido, ajústalo"
    val categoria: String? = null,
    val detalle: String = "",
    /** Ya había un reporte guardado ese día: Guardar pide confirmar que lo reemplaza. */
    val existe: Boolean = false
) {
    val planeadas: Double? get() = leerHoras(textoPlaneadas)
    val horas: Double? get() = leerHoras(texto)
    val pideMotivo: Boolean
        get() {
            val p = planeadas ?: return false
            val h = horas ?: return false
            return h < p
        }
    val motivo: String
        get() {
            val c = categoria ?: return ""
            return if (detalle.isBlank()) c else "$c: ${detalle.trim()}"
        }
    val errorMotivo: String?
        get() = when {
            categoria == null || categoria !in MOTIVOS_HORAS -> "Selecciona un motivo."
            categoria == "Otro" && detalle.isBlank() -> "Escribe el motivo."
            motivo.length > 500 -> "Acorta el detalle a 480 caracteres."
            else -> null
        }
    val puedeGuardar: Boolean
        get() {
            val p = planeadas ?: return false
            val h = horas ?: return false
            if (p == 0.0 && h == 0.0) return false
            return !pideMotivo || errorMotivo == null
        }

    companion object {
        /** Separa "Salud: cita médica" en categoría y detalle; un texto viejo sin categoría queda como "Otro". */
        fun partirMotivo(guardado: String?): Pair<String?, String> {
            val texto = guardado.orEmpty()
            for (c in MOTIVOS_HORAS) {
                if (texto == c) return c to ""
                if (texto.startsWith("$c: ")) return c to texto.removePrefix("$c: ")
            }
            return if (texto.isBlank()) null to "" else "Otro" to texto
        }
    }
}

data class HorasUiState(
    val dias: List<DiaSesion> = emptyList(),
    val fecha: LocalDate = LocalDate.now(),
    val registro: RegistroDiaUi? = null,
    val guardando: Boolean = false,
    val mensaje: String? = null
)

/** Tiempo mínimo que se ve la barra de progreso al guardar. */
private const val DURACION_MINIMA_GUARDADO_MS = 1_200L

class HorasViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = HorasRepository(app)
    private val contexto = ContextoRepository(app)
    private val local = MutableStateFlow(HorasUiState())

    val uiState: StateFlow<HorasUiState> = combine(repo.observarDias(), local) { dias, actual ->
        actual.copy(dias = dias)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HorasUiState())

    init {
        // Una lectura al abrir Horas: actualiza la última hora en la sede, que es el tope de la sugerencia.
        viewModelScope.launch { contexto.registrarPresencia() }
        seleccionarDia(LocalDate.now())
    }

    /** Carga lo guardado ese día; si no hay nada, propone las horas del horario y la sugerencia por presencia. */
    fun seleccionarDia(fecha: LocalDate) {
        local.update { it.copy(fecha = fecha, mensaje = null) }
        viewModelScope.launch {
            val guardado = repo.reporteDelDia(fecha)
            val registro = if (guardado != null) {
                val (categoria, detalle) = RegistroDiaUi.partirMotivo(guardado.motivo)
                RegistroDiaUi(
                    fecha = fecha,
                    textoPlaneadas = formatearHoras(guardado.horasPlaneadas),
                    texto = formatearHoras(guardado.horasRealizadas),
                    origen = guardado.origen,
                    sugerido = guardado.origen == OrigenValor.SUGERIDO,
                    categoria = categoria,
                    detalle = detalle,
                    existe = true
                )
            } else {
                val programadas = repo.horasProgramadas(fecha)
                val sugerencia = repo.sugerencia(fecha, programadas)
                RegistroDiaUi(
                    fecha = fecha,
                    textoPlaneadas = if (programadas > 0) formatearHoras(programadas) else "",
                    texto = sugerencia?.let { formatearHoras(it.horas) }.orEmpty(),
                    origen = if (sugerencia != null) OrigenValor.SUGERIDO else OrigenValor.MANUAL,
                    sugerido = sugerencia != null
                )
            }
            // Si el docente ya tocó otro día mientras cargaba, no se pisa.
            local.update { if (it.fecha == fecha) it.copy(registro = registro) else it }
        }
    }

    fun abrirDia(dia: DiaSesion) = seleccionarDia(dia.fecha)

    fun cambiarPlaneadas(texto: String) = local.update { it.copy(registro = it.registro?.copy(textoPlaneadas = texto), mensaje = null) }

    /** Si el docente toca el valor, deja de ser una sugerencia. */
    fun cambiarHoras(texto: String) =
        local.update { it.copy(registro = it.registro?.copy(texto = texto, origen = OrigenValor.MANUAL, sugerido = false), mensaje = null) }

    fun cambiarCategoria(categoria: String) = local.update { it.copy(registro = it.registro?.copy(categoria = categoria)) }

    fun cambiarDetalle(detalle: String) = local.update { it.copy(registro = it.registro?.copy(detalle = detalle.take(480))) }

    /** Compatibilidad con la hoja anterior: el motivo libre entra como detalle. */
    fun cambiarMotivo(motivo: String) = cambiarDetalle(motivo)

    fun cerrar() = local.update { it.copy(registro = null) }

    fun guardar() {
        val actual = local.value.registro ?: return
        val planeadas = actual.planeadas ?: return
        val horas = actual.horas ?: return
        if (!actual.puedeGuardar || local.value.guardando) return
        local.update { it.copy(guardando = true, mensaje = null) }
        viewModelScope.launch {
            val inicio = System.currentTimeMillis()
            val ok = runCatching {
                repo.guardar(actual.fecha, planeadas, horas, actual.origen, actual.motivo.takeIf { actual.pideMotivo })
            }.isSuccess
            // Guardar en el teléfono es casi instantáneo: se deja la barra de progreso a la vista un momento
            // para que el docente note que algo ocurre (en Flutter dura lo que tarda el envío).
            val restante = DURACION_MINIMA_GUARDADO_MS - (System.currentTimeMillis() - inicio)
            if (restante > 0) delay(restante)
            local.update {
                it.copy(
                    guardando = false,
                    registro = if (ok) actual.copy(existe = true) else it.registro,
                    mensaje = if (ok) "Horas guardadas. Se envían en cuanto haya conexión." else "No pudimos guardar las horas. Intenta de nuevo."
                )
            }
        }
    }
}
