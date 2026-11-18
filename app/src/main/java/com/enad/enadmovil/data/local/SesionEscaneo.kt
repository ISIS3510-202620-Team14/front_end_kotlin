package com.enad.enadmovil.data.local

import android.content.Context
import com.enad.enadmovil.domain.model.EstudianteEscaneado
import com.enad.enadmovil.domain.model.FilaRevisada
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Escaneo sin terminar guardado en el almacenamiento privado de la app (filesDir/escaneo).
 * Ahí quedan las fotos y un JSON con las páginas y las filas que el docente ya revisó. Si
 * cierra la app a mitad del escaneo, al volver a abrir el escáner sigue donde iba.
 * Al importar o cancelar se borra todo, fotos incluidas. Hay un archivo por cuenta y pantalla,
 * así otro docente en el mismo celular no ve el escaneo del anterior.
 */
class SesionEscaneo(private val carpeta: File) {
    constructor(context: Context) : this(carpetaDe(context))

    /** [estado]: "lista", "fallo" o "pendiente". [filas] solo cuenta cuando está lista. */
    data class Pagina(val numero: Int, val ruta: String, val estado: String, val filas: Int = 0)

    data class Datos(
        val siguientePagina: Int,
        val entregadas: Int,           // páginas cuyas filas ya están en la revisión
        val paginas: List<Pagina>,
        val filas: List<FilaRevisada>,
        val borradas: Int
    )

    /** Escribe un archivo, así que se llama desde un hilo de IO. */
    fun guardar(clave: String, datos: Datos) {
        carpeta.mkdirs()
        val destino = archivo(clave)
        val temporal = File(carpeta, "${destino.name}.tmp")
        temporal.writeText(aJson(datos).toString(), Charsets.UTF_8)
        if (!temporal.renameTo(destino)) temporal.delete()
    }

    /** null si no hay escaneo guardado o el archivo está dañado. Se llama desde un hilo de IO. */
    fun leer(clave: String): Datos? = runCatching {
        val archivo = archivo(clave)
        if (!archivo.exists()) null else deJson(JSONObject(archivo.readText(Charsets.UTF_8)))
    }.getOrNull()

    /** Borra el escaneo guardado. Se llama desde un hilo de IO. */
    fun borrar(clave: String) {
        archivo(clave).delete()
    }

    /** Borra fotos del escaneo. Solo borra las que estén dentro de esta carpeta. */
    fun borrarFotos(rutas: List<String>) {
        rutas.map(::File).filter { it.parentFile == carpeta }.forEach { it.delete() }
    }

    private fun archivo(clave: String) = File(carpeta, "sesion_" + clave.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    private fun aJson(d: Datos) = JSONObject()
        .put("siguientePagina", d.siguientePagina)
        .put("entregadas", d.entregadas)
        .put("borradas", d.borradas)
        .put("paginas", JSONArray().apply {
            d.paginas.forEach {
                put(JSONObject().put("numero", it.numero).put("ruta", it.ruta).put("estado", it.estado).put("filas", it.filas))
            }
        })
        .put("filas", JSONArray().apply {
            d.filas.forEach { put(JSONObject().put("leida", filaAJson(it.leida)).put("actual", filaAJson(it.actual))) }
        })

    private fun deJson(o: JSONObject): Datos {
        val paginas = o.getJSONArray("paginas")
        val filas = o.getJSONArray("filas")
        return Datos(
            siguientePagina = o.getInt("siguientePagina"),
            entregadas = o.getInt("entregadas"),
            borradas = o.optInt("borradas"),
            paginas = List(paginas.length()) { i ->
                val p = paginas.getJSONObject(i)
                Pagina(p.getInt("numero"), p.getString("ruta"), p.getString("estado"), p.optInt("filas"))
            },
            filas = List(filas.length()) { i ->
                val f = filas.getJSONObject(i)
                FilaRevisada(filaDeJson(f.getJSONObject("leida")), filaDeJson(f.getJSONObject("actual")))
            }
        )
    }

    private fun filaAJson(f: EstudianteEscaneado) = JSONObject()
        .put("nombre", f.nombre)
        .put("codigo", f.codigo ?: JSONObject.NULL)
        .put("grado", f.grado ?: JSONObject.NULL)
        .put("edad", f.edad ?: JSONObject.NULL)
        .put("presente", f.presente ?: JSONObject.NULL)

    private fun filaDeJson(o: JSONObject) = EstudianteEscaneado(
        nombre = o.getString("nombre"),
        codigo = if (o.isNull("codigo")) null else o.getString("codigo"),
        grado = if (o.isNull("grado")) null else o.getInt("grado"),
        edad = if (o.isNull("edad")) null else o.getInt("edad"),
        presente = if (o.isNull("presente")) null else o.getBoolean("presente")
    )

    companion object {
        const val CARPETA = "escaneo"

        /** Donde la cámara guarda las fotos del escaneo y donde queda el JSON. */
        fun carpetaDe(context: Context) = File(context.applicationContext.filesDir, CARPETA)
    }
}
