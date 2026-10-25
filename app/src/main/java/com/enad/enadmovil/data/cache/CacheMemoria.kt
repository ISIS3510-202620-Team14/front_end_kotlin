package com.enad.enadmovil.data.cache

import android.util.Log
import android.util.LruCache
import java.util.concurrent.atomic.AtomicInteger

class CacheMemoria(maxEntradas: Int,private val tiempoDeVidaMs: Long, private val nombre: String, private val reloj: () -> Long = System::currentTimeMillis) {
    private class Entrada(val valor: Any, val venceEn: Long?)

    private val lru = LruCache<String, Entrada>(maxEntradas)
    private val _aciertos = AtomicInteger()
    private val _fallos = AtomicInteger()

    val aciertos: Int get() = _aciertos.get()
    val fallos: Int get() = _fallos.get()

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> obtener(clave: String): T? {
        val entrada = lru.get(clave)
        if (entrada == null) {
            registrar(clave, "FALLO (no está)", _fallos)
            return null
        }
        if (entrada.venceEn != null && reloj() >= entrada.venceEn) {
            lru.remove(clave)
            registrar(clave, "FALLO (venció)", _fallos)
            return null
        }
        registrar(clave, "ACIERTO", _aciertos)
        return entrada.valor as T
    }

    fun guardar(clave: String, valor: Any, vence: Boolean = true) {
        var instante: Long?  = 0L
        if (vence) {
             instante = reloj() + tiempoDeVidaMs
        } else {
            instante = null
        }
        lru.put(clave, Entrada(valor, instante))
    }

    fun invalidar(clave: String) {
        lru.remove(clave)
    }

    fun limpiar() {
        lru.evictAll()
    }

    suspend fun <T : Any> obtenerOCargar(clave: String, forzar: Boolean = false, vence: (T) -> Boolean = { true }, cargar: suspend () -> T): T {
        if (forzar) {
            invalidar(clave)
            registrar(clave, "FORZADO", _fallos)
        } else {
            obtener<T>(clave)?.let { return it}
        }
        val valor = cargar()
        guardar(clave, valor, vence(valor))
        return valor
    }

    private fun registrar(clave: String, evento: String, contador: AtomicInteger) {
        contador.incrementAndGet()
        Log.d(TAG, "[$nombre] $evento - $clave - aciertos=$aciertos fallos=$fallos")
    }
    private companion object {
        const val TAG = "CacheMemoria"
    }
}