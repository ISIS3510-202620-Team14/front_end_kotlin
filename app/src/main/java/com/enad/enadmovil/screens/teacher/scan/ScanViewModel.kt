package com.enad.enadmovil.ui.screens.teacher.scan

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.cache.CacheMiniaturas
import com.enad.enadmovil.data.imagen.LectorMiniaturas
import com.enad.enadmovil.data.ocr.HilosOcr
import com.enad.enadmovil.data.ocr.TextRecognizerDataSource
import com.enad.enadmovil.data.repository.AuthRepository
import com.enad.enadmovil.data.telemetria.EventosEscaneo
import com.enad.enadmovil.domain.model.CondicionesCaptura
import com.enad.enadmovil.domain.model.EstudianteEscaneado
import com.enad.enadmovil.domain.parser.ListaAsistenciaParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Escáner de varias páginas. La cámara deja cada foto en una cola y un lector por hilo del
 * pool las va tomando: si el docente toma tres fotos seguidas, dos se leen al mismo tiempo
 * y la tercera espera a que se libere un hilo. Mientras tanto la cámara sigue disponible.
 */
class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val hilos = HilosOcr()
    private val ocr = TextRecognizerDataSource(app, hilos.dispatcher.executor)
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()
    private var siguientePagina = 1

    private var fotos = Channel<Foto>(Channel.UNLIMITED)
    // Filas de páginas que terminaron antes que una anterior: esperan para salir en orden
    private val terminadas = mutableMapOf<Int, List<EstudianteEscaneado>>()
    private var entregadas = 0
    private var lectores = iniciarLectores()

    private val auth = AuthRepository()

    private data class Foto(val numero: Int, val uri: Uri, val condiciones: CondicionesCaptura)

    private val avisosDeMemoria = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) = CacheMiniaturas.recortar(level)
        override fun onConfigurationChanged(newConfig: Configuration) = Unit
        @Deprecated("Se maneja en onTrimMemory")
        override fun onLowMemory() = CacheMiniaturas.limpiar()
    }

    init {
        app.registerComponentCallbacks(avisosDeMemoria)
    }

    fun onFotoCapturada(uri: Uri, condiciones: CondicionesCaptura = CondicionesCaptura.SIN_SENSORES) {
        val numero = siguientePagina++
        _uiState.update {
            it.copy(procesando = true, error = null, paginas = it.paginas + PaginaEscaneada(numero, EstadoPagina.EnCola, uri))
        }
        fotos.trySend(Foto(numero, uri, condiciones))
    }

    /**
     * Miniatura de una página. Si ya está en la caché sale de memoria al instante. Si no,
     * se abre achicada desde el disco en un hilo de IO y se guarda para la próxima vez.
     */
    suspend fun miniatura(pagina: PaginaEscaneada): Bitmap? {
        val clave = pagina.foto.toString()
        CacheMiniaturas.obtener(clave)?.let { return it }
        val leida = try {
            withContext(Dispatchers.IO) { LectorMiniaturas.leer(pagina.foto) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // sin miniatura la página se sigue leyendo igual
        } ?: return null
        CacheMiniaturas.guardar(clave, leida)
        return leida
    }

    /** El sheet ya copió las filas nuevas a su lista: se vacían para no sumarlas dos veces. */
    fun tomarFilas() {
        _uiState.update { it.copy(filas = emptyList()) }
    }

    fun limpiar() {
        // Lo que seguía en cola o leyéndose es de un escaneo que ya se cerró: se descarta
        lectores.cancel()
        fotos.cancel()
        fotos = Channel(Channel.UNLIMITED)
        lectores = iniciarLectores()
        siguientePagina = 1
        terminadas.clear()
        entregadas = 0
        // Las miniaturas eran de las fotos de este escaneo y no se vuelven a mostrar
        CacheMiniaturas.limpiar()
        _uiState.value = ScanUiState()
    }

    private fun iniciarLectores(): Job {
        val cola = fotos
        return viewModelScope.launch {
            repeat(hilos.cantidad) {
                launch { for (foto in cola) leer(foto) }
            }
        }
    }

    /**
     * Los lectores viven en el hilo principal y solo saltan al pool para el trabajo pesado.
     * Así el estado siempre se cambia desde el mismo hilo y dos páginas no se pisan.
     */
    private suspend fun leer(foto: Foto) {
        _uiState.update { it.copy(paginas = it.paginas.marcar(foto.numero, EstadoPagina.Leyendo)) }
        val inicio = SystemClock.elapsedRealtime()
        val filas: List<EstudianteEscaneado>? = try {
            withContext(hilos.dispatcher) {
                ListaAsistenciaParser.parsear(ocr.reconocer(foto.uri)).also {
                    // El nombre del hilo muestra en Logcat que cada página se leyó en el pool del escáner
                    Log.d(TAG, "Página ${foto.numero}: ${it.size} filas en ${SystemClock.elapsedRealtime() - inicio} ms (${Thread.currentThread().name})")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // una foto que falla no detiene las demás
        }
        auth.uidActual()?.let { EventosEscaneo.registrar(it, foto.condiciones, filas?.size, SystemClock.elapsedRealtime() - inicio) }
        val enOrden = enOrden(foto.numero, filas.orEmpty())
        _uiState.update {
            val paginas = it.paginas.marcar(foto.numero, filas?.let { f -> EstadoPagina.Lista(f.size) } ?: EstadoPagina.Fallo)
            it.copy(
                paginas = paginas,
                filas = it.filas + enOrden,
                procesando = paginas.any { p -> p.estado == EstadoPagina.EnCola || p.estado == EstadoPagina.Leyendo },
                error = when {
                    filas == null -> "No se pudo leer la página ${foto.numero}."
                    filas.isEmpty() -> "La página ${foto.numero} no tiene filas legibles. Intenta con más luz y la lista de frente."
                    else -> it.error
                }
            )
        }
    }

    /**
     * La página 2 puede terminar antes que la 1 porque se leen al mismo tiempo. Sus filas se
     * guardan hasta que todas las anteriores terminen, y así la lista queda en el orden de las fotos.
     */
    private fun enOrden(numero: Int, filas: List<EstudianteEscaneado>): List<EstudianteEscaneado> {
        terminadas[numero] = filas
        val listas = mutableListOf<EstudianteEscaneado>()
        while (true) {
            val siguiente = terminadas.remove(entregadas + 1) ?: break
            listas += siguiente
            entregadas++
        }
        return listas
    }

    private fun List<PaginaEscaneada>.marcar(numero: Int, estado: EstadoPagina) =
        map { if (it.numero == numero) it.copy(estado = estado) else it }

    override fun onCleared() {
        // Sin esto los hilos del pool y el modelo de ML Kit quedan vivos después de cerrar la pantalla
        fotos.cancel()
        ocr.cerrar()
        hilos.close()
        getApplication<Application>().unregisterComponentCallbacks(avisosDeMemoria)
        super.onCleared()
    }

    private companion object {
        const val TAG = "ScanViewModel"
    }
}
