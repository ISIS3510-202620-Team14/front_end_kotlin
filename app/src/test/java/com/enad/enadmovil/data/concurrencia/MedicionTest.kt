package com.enad.enadmovil.data.concurrencia

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors

class MedicionTest {

    @Test
    fun `medir guarda el resultado y el tiempo que tardo`() = runBlocking {
        val medida = medir("escuelas") { delay(120); 7 }
        assertEquals("escuelas", medida.nombre)
        assertEquals(7, medida.resultado.getOrNull())
        assertTrue("tardó ${medida.ms} ms", medida.ms >= 100)
    }

    @Test
    fun `medir guarda el error sin lanzarlo`() = runBlocking {
        val medida = medir<Int>("gps") { error("sin permiso") }
        assertTrue(medida.resultado.isFailure)
        assertEquals("sin permiso", medida.resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `medir anota el hilo en que arranco`() = runBlocking {
        val ejecutor = Executors.newSingleThreadExecutor { Thread(it, "hilo-de-prueba") }
        try {
            val medida = withContext(ejecutor.asCoroutineDispatcher()) { medir("x") { 1 } }
            assertTrue(medida.hilo, medida.hilo.startsWith("hilo-de-prueba"))
        } finally {
            ejecutor.shutdown()
        }
    }

    @Test
    fun `medir no se traga la cancelacion`() = runBlocking {
        var cancelada = false
        val trabajo = launch {
            try {
                medir("lenta") { delay(5_000) }
            } catch (e: CancellationException) {
                cancelada = true
                throw e
            }
        }
        delay(50)
        trabajo.cancel()
        trabajo.join()
        assertTrue(cancelada)
    }

    @Test
    fun `tres tareas en paralelo tardan lo de la mas lenta y no la suma`() = runBlocking {
        val inicio = System.nanoTime()
        val medidas = coroutineScope {
            listOf("a", "b", "c").map { nombre -> async(Dispatchers.Default) { medir(nombre) { delay(250) } } }.awaitAll()
        }
        val totalMs = (System.nanoTime() - inicio) / 1_000_000
        assertTrue("la suma debe ser ~750 ms y fue ${medidas.sumOf { it.ms }}", medidas.sumOf { it.ms } >= 700)
        assertTrue("en paralelo debe quedar bajo 600 ms y fue $totalMs", totalMs < 600)
    }

    @Test
    fun `si una tarea falla las otras terminan`() = runBlocking {
        val medidas = supervisorScope {
            listOf(
                async { medir("catalogo") { delay(100); "ok" } },
                async { medir<String>("gps") { delay(50); error("sin señal") } },
                async { medir("grupos") { delay(150); "ok" } }
            ).awaitAll()
        }
        assertEquals(listOf(false, true, false), medidas.map { it.resultado.isFailure })
        assertEquals("ok", medidas[2].resultado.getOrNull())
    }

    @Test
    fun `el resumen muestra cada tiempo, el total y los hilos`() {
        val medidas = listOf(
            Medida("escuelas", Result.success(1), 410, "worker-1"),
            Medida("gps", Result.failure<Int>(Exception("x")), 1900, "worker-2"),
            Medida("grupos", Result.success(3), 380, "worker-1")
        )
        assertEquals(
            "Hoy: escuelas 410 ms, gps 1900 ms (falló), grupos 380 ms -> total 1950 ms (en fila: 2690 ms) [worker-1, worker-2]",
            resumenTiempos("Hoy", medidas, 1950)
        )
    }

    @Test
    fun `el resumen sin tareas no falla`() {
        assertFalse(resumenTiempos("Hoy", emptyList(), 0).isBlank())
    }
}