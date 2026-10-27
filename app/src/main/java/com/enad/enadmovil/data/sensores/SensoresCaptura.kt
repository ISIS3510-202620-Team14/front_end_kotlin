package com.enad.enadmovil.data.sensores

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.enad.enadmovil.domain.model.DetectorQuietud
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** Luz (lux) y quietud que miden los sensores mientras la cámara está abierta. null = el celular no tiene ese sensor. */
data class LecturaSensores(val lux: Float?, val estable: Boolean?)

/**
 * Sensores: luz ambiental y acelerómetro, para ayudar a tomar la foto de la lista.
 * Solo escuchan mientras alguien recolecta [lecturas]; al dejar de hacerlo se desregistran
 * y no gastan batería. Si el celular no tiene alguno, ese dato llega en null y la cámara
 * funciona como antes.
 */
class SensoresCaptura(context: Context) {

    private val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    private val sensorLuz: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val acelerometro: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun lecturas(): Flow<LecturaSensores> = callbackFlow {
        val detector = DetectorQuietud()
        // El lux se redondea: el sensor cambia décimas todo el tiempo y no vale la pena repintar por eso
        var lux: Float? = null
        var estable: Boolean? = if (acelerometro != null) false else null
        trySend(LecturaSensores(lux, estable))

        val oyente = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent) {
                when (evento.sensor.type) {
                    Sensor.TYPE_LIGHT -> lux = Math.round(evento.values[0]).toFloat()
                    Sensor.TYPE_ACCELEROMETER -> estable = detector.agregar(
                        evento.values[0], evento.values[1], evento.values[2],
                        evento.timestamp / 1_000_000 // ns -> ms
                    )
                }
                trySend(LecturaSensores(lux, estable))
            }

            override fun onAccuracyChanged(sensor: Sensor, precision: Int) = Unit
        }
        sensorLuz?.let { manager?.registerListener(oyente, it, SensorManager.SENSOR_DELAY_UI) }
        acelerometro?.let { manager?.registerListener(oyente, it, SensorManager.SENSOR_DELAY_UI) }
        awaitClose { manager?.unregisterListener(oyente) }
    }.conflate().distinctUntilChanged()
}
