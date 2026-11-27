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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Mapa de asistencia del admin. Primero muestra lo que hay guardado (RAM o disco) y luego
 * lo que llega de la red. Si la red falla se queda con lo guardado.
 */
class MapaAsistenciaRepository(
    private val archivo: ArchivoRespuestas,
    private val auth: AuthRepository = AuthRepository(),
    private val hoy: () -> LocalDate = { LocalDate.now(ZONA) }
) {
    enum class Origen { CACHE, RED, DISCO }

    data class Resultado(val mapa: MapaAsistencia, val origen: Origen)

    fun cargar(): Flow<Resultado> = flow {
        val guardado = guardado()
        if (guardado != null) {
            Log.d(TAG, "Cache then network: muestra lo guardado mientras llega la red")
            emit(Resultado(guardado, Origen.CACHE))
        }
        val nuevo = try {
            descargar()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Falló la red: ${e.message}")
            if (guardado == null) throw e
            emit(Resultado(guardado, Origen.DISCO))
            return@flow
        }
        cache.guardar(clave(), nuevo, vence = false)
        emit(Resultado(nuevo, Origen.RED))
    }

    private suspend fun guardado(): MapaAsistencia? {
        cache.obtener<MapaAsistencia>(clave())?.let { return it }
        val uid = auth.uidActual() ?: return null
        val leido = archivo.leer(uid, NOMBRE) ?: return null
        Log.d(TAG, "Copia en disco: ACIERTO (${leido.json.length} bytes)")
        return runCatching { mapaAsistenciaDeJson(JSONObject(leido.json)) }.getOrNull()
    }

    private suspend fun descargar(): MapaAsistencia {
        val token = token()
        val hoy = hoy()
        val lunes = hoy.with(DayOfWeek.MONDAY)
        val dias = (0L..4L).map { lunes.plusDays(it).toString() }
        val semanaPasada = (-7L..-3L).map { lunes.plusDays(it).toString() }
        // Los días que todavía no llegan no se piden
        val pedidos = semanaPasada + dias.filter { it <= hoy.toString() }
        val escuelas = CloudFunctionsApi.listarEscuelas(token)

        val inicio = System.nanoTime()
        val turnos = Semaphore(PETICIONES_A_LA_VEZ)
        val marcas = coroutineScope {
            escuelas.map { escuela ->
                async {
                    val listas = pedidos.map { fecha ->
                        async { turnos.withPermit { marcasDelDia(token, escuela.id, fecha) } }
                    }.awaitAll()
                    MarcasInstitucion(
                        nombre = escuela.nombre,
                        estaSemana = listas.drop(5) + List(10 - listas.size) { emptyList() },
                        semanaPasada = listas.take(5)
                    )
                }
            }.awaitAll()
        }
        Log.d(TAG, "${escuelas.size * pedidos.size} peticiones en paralelo: ${(System.nanoTime() - inicio) / 1_000_000} ms")

        val mapa = withContext(Dispatchers.Default) { armarMapaAsistencia(dias, marcas, System.currentTimeMillis()) }
        auth.uidActual()?.let { uid -> runCatching { archivo.guardar(uid, NOMBRE, mapa.aJson().toString()) } }
        return mapa
    }

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
    }
}
