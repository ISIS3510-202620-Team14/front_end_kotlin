package com.enad.enadmovil.ui.screens.teacher.scan

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.cache.CacheMiniaturas
import com.enad.enadmovil.data.imagen.LectorMiniaturas
import com.enad.enadmovil.data.local.SesionEscaneo
import com.enad.enadmovil.data.ocr.HilosOcr
import com.enad.enadmovil.data.ocr.TextRecognizerDataSource
import com.enad.enadmovil.data.repository.AuthRepository
import com.enad.enadmovil.data.telemetria.EventosEscaneo
import com.enad.enadmovil.domain.model.CondicionesCaptura
import com.enad.enadmovil.domain.model.FilaRevisada
import com.enad.enadmovil.domain.model.ResumenRevision
import com.enad.enadmovil.domain.model.EstudianteEscaneado
import com.enad.enadmovil.domain.parser.FusionFilas
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
import java.io.File

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

    /**
     * Filas que el docente está revisando, con sus correcciones. Viven aquí y no en el sheet:
     * si el sheet se cierra sin querer, al volver a abrirlo siguen ahí.
     */
    val revision = mutableStateListOf<FilaRevisada>()
    var borradas by mutableIntStateOf(0)
        private set

    private var fotos = Channel<Foto>(Channel.UNLIMITED)
    // Filas de páginas que terminaron antes que una anterior: esperan para salir en orden
    private val terminadas = mutableMapOf<Int, List<EstudianteEscaneado>>()
    private var entregadas = 0
    private var lectores = iniciarLectores()

    private val auth = AuthRepository()

    // Escaneo sin terminar en disco, para que sobreviva a cerrar la app
    private val sesion = SesionEscaneo(app)
    private var claveSesion: String? = null
    private val escrituras = Channel<Escritura>(Channel.CONFLATED)

    private sealed interface Escritura {
        data class Guardar(val clave: String, val datos: SesionEscaneo.Datos) : Escritura
        data class Borrar(val clave: String) : Escritura
    }

    // [releida]: página que se vuelve a leer al restaurar un escaneo, no cuenta otra vez para la BQ
    private data class Foto(val numero: Int, val uri: Uri, val condiciones: CondicionesCaptura, val releida: Boolean = false)

    private val avisosDeMemoria = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) = CacheMiniaturas.recortar(level)
        override fun onConfigurationChanged(newConfig: Configuration) = Unit
        @Deprecated("Se maneja en onTrimMemory")
        override fun onLowMemory() = CacheMiniaturas.limpiar()
    }

    init {
        app.registerComponentCallbacks(avisosDeMemoria)
        CacheMiniaturas.usarCarpeta(File(app.filesDir, CARPETA_MINIATURAS))
        // Un solo escritor en IO. El canal se queda con el último cambio: si llegan varios
        // seguidos (el docente escribiendo un nombre) solo se escribe el más nuevo.
        viewModelScope.launch(Dispatchers.IO) {
            for (e in escrituras) {
                runCatching {
                    when (e) {
                        is Escritura.Guardar -> sesion.guardar(e.clave, e.datos)
                        is Escritura.Borrar -> sesion.borrar(e.clave)
                    }
                }
            }
        }
    }

    /**
     * El sheet avisa desde qué pantalla se abrió. Si esta cuenta dejó un escaneo sin terminar
     * ahí (por ejemplo cerró la app), se recupera con sus páginas y sus filas.
     */
    fun abrir(origen: String) {
        if (claveSesion != null) return
        val clave = "${auth.uidActual() ?: return}_$origen"
        claveSesion = clave
        viewModelScope.launch {
            val datos = withContext(Dispatchers.IO) { sesion.leer(clave) } ?: return@launch
            // Si el docente ya empezó a escanear antes de que terminara de leer, gana lo nuevo
            if (siguientePagina == 1 && revision.isEmpty()) restaurar(datos)
        }
    }

    private fun restaurar(datos: SesionEscaneo.Datos) {
        revision.addAll(datos.filas)
        borradas = datos.borradas
        siguientePagina = datos.siguientePagina
        entregadas = datos.entregadas
        // Las páginas que no alcanzaron a entregar sus filas se vuelven a leer
        val paginas = datos.paginas.map { p ->
            val estado = when {
                p.numero > datos.entregadas -> EstadoPagina.EnCola
                p.estado == "fallo" -> EstadoPagina.Fallo
                else -> EstadoPagina.Lista(p.filas)
            }
            PaginaEscaneada(p.numero, estado, Uri.fromFile(File(p.ruta)))
        }
        _uiState.value = ScanUiState(procesando = paginas.any { it.estado == EstadoPagina.EnCola }, paginas = paginas)
        paginas.filter { it.estado == EstadoPagina.EnCola }
            .forEach { fotos.trySend(Foto(it.numero, it.foto, CondicionesCaptura.SIN_SENSORES, releida = true)) }
        Log.d(TAG, "Escaneo recuperado: ${paginas.size} página(s), ${revision.size} fila(s)")
    }

    /** Toma una foto del estado actual y la manda al escritor de IO. */
    private fun guardarSesion() {
        val clave = claveSesion ?: return
        val paginas = _uiState.value.paginas.mapNotNull { p ->
            val ruta = p.foto.path ?: return@mapNotNull null
            when (val e = p.estado) {
                is EstadoPagina.Lista -> SesionEscaneo.Pagina(p.numero, ruta, "lista", e.filas)
                EstadoPagina.Fallo -> SesionEscaneo.Pagina(p.numero, ruta, "fallo")
                else -> SesionEscaneo.Pagina(p.numero, ruta, "pendiente")
            }
        }
        escrituras.trySend(Escritura.Guardar(clave, SesionEscaneo.Datos(siguientePagina, entregadas, paginas, revision.toList(), borradas)))
    }

    fun onFotoCapturada(uri: Uri, condiciones: CondicionesCaptura = CondicionesCaptura.SIN_SENSORES) {
        val numero = siguientePagina++
        _uiState.update {
            it.copy(procesando = true, error = null, paginas = it.paginas + PaginaEscaneada(numero, EstadoPagina.EnCola, uri))
        }
        fotos.trySend(Foto(numero, uri, condiciones))
        guardarSesion()
    }

    /**
     * Miniatura de una página. Primero se busca en memoria, que es instantáneo. Si no está, se
     * busca la miniatura guardada en disco. Solo si tampoco está se abre la foto completa
     * achicada y se guarda en los dos niveles para la próxima vez. Lo de disco va en un hilo de IO.
     */
    suspend fun miniatura(pagina: PaginaEscaneada): Bitmap? {
        val clave = pagina.foto.toString()
        CacheMiniaturas.obtener(clave)?.let { return it }
        return try {
            withContext(Dispatchers.IO) {
                CacheMiniaturas.leerDeDisco(clave)
                    ?: LectorMiniaturas.leer(pagina.foto)?.also { CacheMiniaturas.guardar(clave, it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // sin miniatura la página se sigue leyendo igual
        }
    }

    /** BQ #11: lo que corrigió el docente en la revisión. [exigirCodigo] = vino de Gestionar estudiantes. */
    fun registrarRevision(resumen: ResumenRevision, exigirCodigo: Boolean) {
        auth.uidActual()?.let { EventosEscaneo.registrarRevision(it, resumen, if (exigirCodigo) "manage_students" else "attendance") }
    }

    fun editarFila(indice: Int, fila: EstudianteEscaneado) {
        revision[indice] = revision[indice].copy(actual = fila)
        guardarSesion()
    }

    fun quitarFila(indice: Int) {
        revision.removeAt(indice)
        borradas++
        guardarSesion()
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
        revision.clear()
        borradas = 0
        // Las miniaturas eran de las fotos de este escaneo y no se vuelven a mostrar
        val claves = _uiState.value.paginas.map { it.foto.toString() }
        CacheMiniaturas.limpiar()
        viewModelScope.launch(Dispatchers.IO) { CacheMiniaturas.borrarDeDisco(claves) }
        // Importado o cancelado: el escaneo guardado y sus fotos ya no se necesitan.
        // Las fotos se borran aparte porque un escaneo nuevo puede reemplazar el borrado en el canal
        val rutas = _uiState.value.paginas.mapNotNull { it.foto.path }
        viewModelScope.launch(Dispatchers.IO) { sesion.borrarFotos(rutas) }
        claveSesion?.let { escrituras.trySend(Escritura.Borrar(it)) }
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
        if (!foto.releida) {
            auth.uidActual()?.let { EventosEscaneo.registrar(it, foto.condiciones, filas?.size, SystemClock.elapsedRealtime() - inicio) }
        }
        val enOrden = enOrden(foto.numero, filas.orEmpty())
        // Las filas nuevas se suman a la revisión sin repetir estudiantes de páginas anteriores
        revision.addAll(FusionFilas.nuevas(revision.map { it.actual }, enOrden).map { FilaRevisada(it) })
        _uiState.update {
            val paginas = it.paginas.marcar(foto.numero, filas?.let { f -> EstadoPagina.Lista(f.size) } ?: EstadoPagina.Fallo)
            it.copy(
                paginas = paginas,
                procesando = paginas.any { p -> p.estado == EstadoPagina.EnCola || p.estado == EstadoPagina.Leyendo },
                error = when {
                    filas == null -> "No se pudo leer la página ${foto.numero}."
                    filas.isEmpty() -> "La página ${foto.numero} no tiene filas legibles. Intenta con más luz y la lista de frente."
                    else -> it.error
                }
            )
        }
        guardarSesion()
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
        const val CARPETA_MINIATURAS = "miniaturas"
    }
}
