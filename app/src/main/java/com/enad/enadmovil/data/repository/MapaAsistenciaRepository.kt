package com.enad.enadmovil.data.repository

import android.util.Log
import com.enad.enadmovil.data.cache.CacheMemoria
import com.enad.enadmovil.data.local.ArchivoRespuestas
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.domain.model.MapaAsistencia
import com.enad.enadmovil.domain.model.MarcasInstitucion
import com.enad.enadmovil.domain.model.aJson
import com.enad.enadmovil.domain.model.armarMapaAsistencia
import com.enad.enadmovil.domain.model.mapaAsistenciaDeJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mapa de asistencia del admin. Primero muestra lo que hay guardado (RAM o disco) y luego
 * lo que llega de la red. Si la red falla se queda con lo guardado.
 */
class MapaAsistenciaRepository(
    private val archivo: ArchivoRespuestas,
    private val auth: AuthRepository = AuthRepository(),
    private val hoy: () -> LocalDate = { LocalDate.now(ZONA) }
) {
    enum class Origen { CACHE, PARCIAL, RED, DISCO }

    /** [cargadas] y [total] solo se usan en PARCIAL: cuántas instituciones ya llegaron. */
    data class Resultado(val mapa: MapaAsistencia, val origen: Origen, val cargadas: Int = 0, val total: Int = 0)

    private data class Avance(val mapa: MapaAsistencia, val cargadas: Int, val total: Int)

    private class Descarga(val avance: MutableStateFlow<Avance?>, val mapa: Deferred<MapaAsistencia>)

    fun cargar(): Flow<Resultado> = channelFlow {
        val guardado = guardado()
        if (guardado != null) {
            Log.d(TAG, "Cache then network: muestra lo guardado mientras llega la red")
            send(Resultado(guardado, Origen.CACHE))
        }
        val descarga = descargaCompartida()
        // Sin copia guardada la tabla se va llenando. Con copia se deja la copia hasta que llegue todo
        val parcial = if (guardado == null) launch {
            descarga.avance.filterNotNull().collect { send(Resultado(it.mapa, Origen.PARCIAL, it.cargadas, it.total)) }
        } else null
        val nuevo = try {
            descarga.mapa.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            parcial?.cancelAndJoin()
            Log.d(TAG, "Falló la red: ${e.message}")
            if (guardado == null) throw e
            send(Resultado(guardado, Origen.DISCO))
            return@channelFlow
        }
        parcial?.cancelAndJoin()
        cache.guardar(clave(), nuevo, vence = false)
        send(Resultado(nuevo, Origen.RED))
    }

    private suspend fun guardado(): MapaAsistencia? {
        cache.obtener<MapaAsistencia>(clave())?.let { return it }
        val uid = auth.uidActual() ?: return null
        val leido = archivo.leer(uid, NOMBRE) ?: return null
        Log.d(TAG, "Copia en disco: ACIERTO (${leido.json.length} bytes)")
        return runCatching { mapaAsistenciaDeJson(JSONObject(leido.json)) }.getOrNull()
    }

    /*
     * Caching de la descarga en curso (request coalescing): si otra pantalla pide el mapa
     * mientras ya se está descargando (Resumen y el reporte a la vez), se espera esa misma
     * descarga en vez de abrir otra. La entrada solo vive mientras la descarga corre.
     */
    private fun descargaCompartida(): Descarga {
        val clave = auth.uidActual() ?: "sin-sesion"
        val avance = MutableStateFlow<Avance?>(null)
        val nueva = Descarga(avance, alcance.async(start = CoroutineStart.LAZY) { descargar(avance) })
        val enCursoYa = enCurso.putIfAbsent(clave, nueva)
        if (enCursoYa != null) {
            nueva.mapa.cancel()
            Log.d(TAG, "Descarga compartida: ACIERTO, se espera la que ya está en curso (aciertos=${aciertos.incrementAndGet()} fallos=${fallos.get()})")
            return enCursoYa
        }
        Log.d(TAG, "Descarga compartida: FALLO, se abre una descarga nueva (aciertos=${aciertos.get()} fallos=${fallos.incrementAndGet()})")
        nueva.mapa.invokeOnCompletion { enCurso.remove(clave, nueva) }
        nueva.mapa.start()
        return nueva
    }

    /*
     * Pipeline de dos etapas con Flow. El productor descarga en hilos de red (flowOn IO) y suelta
     * cada institución apenas terminan sus días. El consumidor corre en hilos de cálculo (Default)
     * y arma la tabla con lo que va llegando. El buffer entre los dos deja que el productor siga
     * descargando mientras el consumidor calcula.
     */
    private suspend fun descargar(avance: MutableStateFlow<Avance?>): MapaAsistencia {
        val token = token()
        val hoy = hoy()
        val lunes = hoy.with(DayOfWeek.MONDAY)
        val dias = (0L..4L).map { lunes.plusDays(it).toString() }
        val semanaPasada = (-7L..-3L).map { lunes.plusDays(it).toString() }
        // Los días que todavía no llegan no se piden
        val pedidos = semanaPasada + dias.filter { it <= hoy.toString() }
        val desde = System.nanoTime()
        val escuelas = CloudFunctionsApi.listarEscuelas(token)

        val inicio = System.nanoTime()
        val turnos = Semaphore(PETICIONES_A_LA_VEZ)

        // Etapa 1, productor: una corrutina por institución, todas en hilos de red
        val llegadas = channelFlow {
            escuelas.forEachIndexed { posicion, escuela ->
                launch {
                    val listas = coroutineScope {
                        pedidos.map { fecha ->
                            async { turnos.withPermit { marcasDelDia(token, escuela.id, fecha) } }
                        }.awaitAll()
                    }
                    val marcas = MarcasInstitucion(
                        nombre = escuela.nombre,
                        estaSemana = listas.drop(5) + List(10 - listas.size) { emptyList() },
                        semanaPasada = listas.take(5)
                    )
                    send(Llegada(posicion, marcas, Thread.currentThread().name))
                }
            }
        }
            .buffer(Channel.UNLIMITED)
            .flowOn(Dispatchers.IO)

        // Etapa 2, consumidor: arma la tabla en hilos de cálculo mientras siguen llegando
        var primera = -1L
        val mapa = withContext(Dispatchers.Default) {
            val puestos = arrayOfNulls<MarcasInstitucion>(escuelas.size)
            var cargadas = 0
            llegadas.collect { llegada ->
                puestos[llegada.posicion] = llegada.marcas
                cargadas++
                val parcial = armarMapaAsistencia(dias, puestos.filterNotNull(), System.currentTimeMillis())
                avance.value = Avance(parcial, cargadas, escuelas.size)
                val ms = (System.nanoTime() - desde) / 1_000_000
                if (primera < 0) primera = ms
                Log.d(TAG, "Pipeline: ${llegada.marcas.nombre} ($cargadas de ${escuelas.size}) a los $ms ms, productor ${llegada.hilo}, consumidor ${Thread.currentThread().name}")
            }
            armarMapaAsistencia(dias, puestos.filterNotNull(), System.currentTimeMillis())
        }
        Log.d(TAG, "${escuelas.size * pedidos.size} peticiones en paralelo: ${(System.nanoTime() - inicio) / 1_000_000} ms")
        val total = (System.nanoTime() - desde) / 1_000_000
        Log.d(TAG, "Tiempos: primera institución en pantalla a los ${if (primera < 0) total else primera} ms, mapa completo a los $total ms (${escuelas.size} instituciones)")
        auth.uidActual()?.let { uid -> runCatching { archivo.guardar(uid, NOMBRE, mapa.aJson().toString()) } }
        return mapa
    }

    private class Llegada(val posicion: Int, val marcas: MarcasInstitucion, val hilo: String)

    private suspend fun marcasDelDia(token: String, schoolId: String, fecha: String): List<String?> =
        CloudFunctionsApi.listarEstudiantes(token, schoolId, fecha).map { it.attendance }

    private fun clave() = "mapa:${auth.uidActual()}"

    private suspend fun token(): String =
        auth.obtenerToken()
            ?: throw CloudFunctionsApi.ApiException("unauthenticated", "Inicia sesión de nuevo para ver el reporte.", 401)

    companion object {
        private const val TAG = "MapaAsistencia"
        private const val NOMBRE = "mapa_asistencia"
        private const val PETICIONES_A_LA_VEZ = 8
        private val ZONA = ZoneId.of("America/Bogota")
        private val cache = CacheMemoria(4, 5 * 60 * 1000L, "mapa-asistencia")

        // Descargas que están corriendo, una por usuario. Corren en su propio alcance para que
        // si la primera pantalla se cierra, la que se enganchó igual reciba el mapa.
        private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val enCurso = ConcurrentHashMap<String, Descarga>()
        private val aciertos = AtomicInteger()
        private val fallos = AtomicInteger()
    }
}
