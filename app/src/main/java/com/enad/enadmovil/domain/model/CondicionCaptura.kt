package com.enad.enadmovil.domain.model

import kotlin.math.sqrt

/**
 * Qué tanta luz hay al tomar la foto de la lista, según el sensor de luz ambiental (lux).
 * El sensor está en la cara de la pantalla: con el celular sobre la hoja mide la luz del
 * salón, que es la misma que le llega al papel.
 */
enum class NivelLuz(val clave: String) {
    OSCURO("dark"),        // el OCR casi no lee: se prende la linterna
    BAJA("low"),           // se lee, pero con errores
    BUENA("good"),
    EXCESIVA("glare"),     // sol directo: la hoja brilla y se pierden letras
    DESCONOCIDA("unknown"); // el celular no tiene sensor de luz

    companion object {
        const val LUX_OSCURO = 15f
        const val LUX_BAJA = 80f
        const val LUX_EXCESIVA = 20_000f

        fun de(lux: Float?): NivelLuz = when {
            lux == null -> DESCONOCIDA
            lux < LUX_OSCURO -> OSCURO
            lux < LUX_BAJA -> BAJA
            lux < LUX_EXCESIVA -> BUENA
            else -> EXCESIVA
        }
    }
}

/** Lo que medían los sensores en el momento de la foto. Se guarda con cada página para la BQ. */
data class CondicionesCaptura(
    val lux: Float?,
    val nivelLuz: NivelLuz,
    val estable: Boolean?,      // null = el celular no tiene acelerómetro
    val linterna: Boolean,
    val automatica: Boolean     // la tomó el disparo automático, no el botón
) {
    companion object {
        val SIN_SENSORES = CondicionesCaptura(null, NivelLuz.DESCONOCIDA, null, linterna = false, automatica = false)
    }
}

/**
 * Dice si el celular está quieto a partir del acelerómetro. Un filtro pasa-bajos estima la
 * gravedad; lo que sobra es movimiento de la mano. El celular está quieto cuando ese
 * movimiento se mantiene bajo [umbral] (m/s²) durante [ventanaMs] seguidos.
 */
class DetectorQuietud(
    private val umbral: Float = 0.35f,
    private val ventanaMs: Long = 600,
    private val alfa: Float = 0.8f
) {
    private val gravedad = FloatArray(3)
    private var iniciado = false
    private var quietoDesde: Long? = null

    /** Agrega una lectura (m/s², instante en ms) y responde si el celular ya lleva [ventanaMs] quieto. */
    fun agregar(x: Float, y: Float, z: Float, instanteMs: Long): Boolean {
        if (!iniciado) {
            gravedad[0] = x; gravedad[1] = y; gravedad[2] = z
            iniciado = true
        }
        gravedad[0] = alfa * gravedad[0] + (1 - alfa) * x
        gravedad[1] = alfa * gravedad[1] + (1 - alfa) * y
        gravedad[2] = alfa * gravedad[2] + (1 - alfa) * z
        val dx = x - gravedad[0]
        val dy = y - gravedad[1]
        val dz = z - gravedad[2]
        val movimiento = sqrt(dx * dx + dy * dy + dz * dz)

        if (movimiento > umbral) {
            quietoDesde = null
            return false
        }
        val desde = quietoDesde ?: instanteMs.also { quietoDesde = it }
        return instanteMs - desde >= ventanaMs
    }

    fun reiniciar() {
        iniciado = false
        quietoDesde = null
    }
}
