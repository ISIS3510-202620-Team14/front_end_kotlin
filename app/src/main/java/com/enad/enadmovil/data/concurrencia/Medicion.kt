package com.enad.enadmovil.data.concurrencia

import kotlinx.coroutines.CancellationException

data class Medida<T>(val nombre: String, val resultado: Result<T>, val ms: Long, val hilo: String)

suspend fun <T> medir(nombre: String, bloque: suspend () -> T): Medida<T> {
    val hilo = Thread.currentThread().name
    val inicio = System.nanoTime()
    val resultado = try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
    return Medida(nombre, resultado, (System.nanoTime() - inicio) / 1_000_000, hilo)
}

fun resumenTiempos(titulo: String, medidas: List<Medida<*>>, totalMs: Long): String {
    val partes = medidas.joinToString(", ") { "${it.nombre} ${it.ms} ms${if (it.resultado.isFailure) " (falló)" else ""}"  }
    val hilos = medidas.map { it.hilo }.distinct().joinToString(", ")
    return "$titulo: $partes -> total $totalMs ms (en fila: ${medidas.sumOf { it.ms}} ms) [$hilos]"
}