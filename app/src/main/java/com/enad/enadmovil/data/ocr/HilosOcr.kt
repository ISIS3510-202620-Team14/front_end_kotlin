package com.enad.enadmovil.data.ocr

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Hilos propios para leer las páginas escaneadas. Leer una foto (abrirla, pasarle el OCR y
 * armar las filas) es trabajo pesado de CPU, así que no corre en el hilo principal ni en los
 * de red. Con un pool fijo se leen varias páginas a la vez sin quitarle CPU a la cámara ni
 * a la interfaz.
 */
class HilosOcr(val cantidad: Int = cantidadDeHilos()) : AutoCloseable {

    private val contador = AtomicInteger(1)

    // Nombre fijo por hilo ("enad-ocr-1", "enad-ocr-2") para reconocerlos en Logcat y en el profiler.
    private val fabrica = ThreadFactory { tarea ->
        Thread(tarea, "enad-ocr-${contador.getAndIncrement()}").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1 // la interfaz va primero
        }
    }

    val dispatcher: ExecutorCoroutineDispatcher =
        Executors.newFixedThreadPool(cantidad, fabrica).asCoroutineDispatcher()

    override fun close() = dispatcher.close()

    companion object {
        /** Un hilo menos que los núcleos del celular (el principal necesita uno), entre 1 y 2. */
        fun cantidadDeHilos(): Int =
            (Runtime.getRuntime().availableProcessors() - 1).coerceIn(1, 2)
    }
}
