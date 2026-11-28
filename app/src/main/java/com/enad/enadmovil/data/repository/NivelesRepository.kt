package com.enad.enadmovil.data.repository

import android.content.Context
import android.util.Log
import com.enad.enadmovil.data.cache.CacheArchivosNiveles
import com.enad.enadmovil.data.local.niveles.NivelesDao
import com.enad.enadmovil.data.local.niveles.NivelesDatabase
import com.enad.enadmovil.data.niveles.EscuelaNivel
import com.enad.enadmovil.data.niveles.FuenteNiveles
import com.enad.enadmovil.data.niveles.FuenteNivelesRed
import com.enad.enadmovil.data.niveles.contarEnSegundoPlano
import com.enad.enadmovil.data.niveles.leerEscuelas
import com.enad.enadmovil.domain.model.FilaNiveles
import kotlinx.coroutines.CancellationException

/** Los números de una descarga, para los logs y la wiki. */
data class DescargaNiveles(
    val total: Int,
    val fallidas: Int,
    val peticiones: Int,
    val desdeCache: Int,
    val sqliteMs: Double,
    val ms: Long,
    val iniciadaEn: Long
)

/** BQ de niveles: view model -> repositorio -> (archivo de caché | red) -> Dispatchers.Default -> SQLite. */
class NivelesRepository(
    private val dao: NivelesDao,
    private val cache: CacheArchivosNiveles,
    private val red: FuenteNiveles,
    private val aEscuelas: (String) -> List<EscuelaNivel> = ::leerEscuelas,
    private val contar: suspend (String) -> IntArray = { contarEnSegundoPlano(it) },
    private val reloj: () -> Long = System::currentTimeMillis,
    private val registro: (String) -> Unit = { Log.d(TAG, it) }
) {
    constructor(context: Context) : this(
        NivelesDatabase.obtener(context).nivelesDao(),
        CacheArchivosNiveles(context),
        FuenteNivelesRed()
    )

    /** Lo que muestra la pantalla: siempre se lee de SQLite, así que también funciona sin internet. */
    suspend fun filas(uid: String, materia: String, grado: Int): List<FilaNiveles> {
        val inicio = System.nanoTime()
        val filas = dao.filas(uid, materia, grado)
        registro("SQLite: ${filas.size} instituciones ($materia, grado ${if (grado == 0) "todos" else grado}) en ${(System.nanoTime() - inicio) / 1_000_000} ms")
        return filas
    }

    /**
     * Una institución después de otra, para no saturar una conexión rural débil: caché o red ->
     * conteo en Dispatchers.Default -> SQLite -> progreso. Si una falla, conserva lo que tenía
     * guardado y sigue la siguiente. [forzar] ignora la caché (botón Actualizar).
     */
    suspend fun descargar(
        uid: String,
        forzar: Boolean,
        alProgresar: (hechas: Int, total: Int) -> Unit = { _, _ -> }
    ): DescargaNiveles {
        val inicioNanos = System.nanoTime()
        val iniciadaEn = reloj()
        var peticiones = 0
        var desdeCache = 0
        var hechas = 0
        var fallidas = 0
        var sqliteNanos = 0L

        // CACHING: caché y, si no hay o venció, red
        suspend fun cacheORed(clave: String, etiqueta: String, pedir: suspend () -> String): String {
            if (!forzar) {
                cache.fresco(clave)?.let {
                    desdeCache++
                    registro("Caché: $etiqueta (archivo vigente, no se pide a la red)")
                    return it
                }
            }
            peticiones++
            registro("Red: $etiqueta")
            val cuerpo = pedir()
            cache.guardar(clave, cuerpo)
            return cuerpo
        }

        val escuelas = aEscuelas(cacheORed("${uid}_instituciones", "instituciones") { red.escuelasJson() })
        alProgresar(0, escuelas.size)

        for (escuela in escuelas) {
            try {
                val cuerpo = cacheORed("${uid}_estudiantes_${escuela.id}", escuela.nombre) { red.estudiantesJson(escuela.id) }
                val conteo = System.nanoTime()
                val conteos = contar(cuerpo)
                registro("Dispatchers.Default: ${escuela.nombre} contada en ${(System.nanoTime() - conteo) / 1_000_000} ms")
                val sqlite = System.nanoTime()
                dao.guardarInstitucion(uid, escuela.id, escuela.nombre, conteos, iniciadaEn)
                sqliteNanos += System.nanoTime() - sqlite
                hechas++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fallidas++
                registro("${escuela.nombre}: no se actualizó (${e.message})")
            }
            alProgresar(hechas + fallidas, escuelas.size)
        }
        // Solo con la descarga completa se sabe qué instituciones ya no existen
        if (fallidas == 0) dao.borrarAnterioresA(uid, iniciadaEn)

        val resultado = DescargaNiveles(
            total = escuelas.size,
            fallidas = fallidas,
            peticiones = peticiones,
            desdeCache = desdeCache,
            sqliteMs = sqliteNanos / 1_000_000.0,
            ms = (System.nanoTime() - inicioNanos) / 1_000_000,
            iniciadaEn = iniciadaEn
        )
        registro(
            "TOTAL: ${resultado.total} instituciones en ${resultado.ms} ms · peticiones=${resultado.peticiones} · " +
                "desde caché=${resultado.desdeCache} · SQLite=${"%.1f".format(resultado.sqliteMs)} ms · fallidas=${resultado.fallidas}"
        )
        return resultado
    }

    private companion object {
        const val TAG = "niveles"
    }
}
