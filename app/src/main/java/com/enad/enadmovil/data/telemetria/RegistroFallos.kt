package com.enad.enadmovil.data.telemetria

import android.content.Context
import android.os.Build
import android.util.Log
import com.enad.enadmovil.data.remote.FirebaseModule
import com.google.firebase.firestore.FieldValue
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BQ tipo 1: qué porcentaje de las sesiones terminan en un fallo y en qué pantalla, versión,
 * modelo y Android pasa. Cada arranque es una sesión ("app_session"). Cuando la app se cae
 * el fallo se guarda en el celular, porque ya no alcanza a subirlo, y se sube como "app_crash"
 * la siguiente vez que se abre con sesión iniciada.
 */
object RegistroFallos {
    private const val TAG = "RegistroFallos"
    private const val CARPETA = "fallos"

    val sesionId: String = UUID.randomUUID().toString()

    /** Ruta de la pantalla que está al frente, la pone la navegación. */
    @Volatile
    var pantalla: String = "login"

    private val instalado = AtomicBoolean(false)
    private val sesionEnviada = AtomicBoolean(false)
    private var carpeta: File? = null
    private var version: String = "0"

    fun instalar(context: Context) {
        if (!instalado.compareAndSet(false, true)) return
        carpeta = File(context.applicationContext.filesDir, CARPETA)
        version = runCatching { InfoDispositivo.versionApp(context) }.getOrDefault("0")
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
            runCatching { guardar(error) }
            anterior?.uncaughtException(hilo, error)
        }
    }

    // Se escribe directo en disco: el proceso muere justo después
    private fun guardar(error: Throwable) {
        val destino = carpeta ?: return
        destino.mkdirs()
        val ahora = System.currentTimeMillis()
        val fallo = JSONObject()
            .put("sessionId", sesionId)
            .put("screen", pantalla)
            .put("error", error.javaClass.name)
            .put("appVersion", version)
            .put("crashedAt", ahora)
        File(destino, "fallo_$ahora.json").writeText(fallo.toString())
    }

    /** Se llama al arrancar y al entrar: registra la sesión y sube los fallos guardados. */
    @Synchronized
    fun subirPendientes() {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return
        if (sesionEnviada.compareAndSet(false, true)) {
            enviar(uid, "app_session", mapOf("sessionId" to sesionId, "appVersion" to version))
        }
        val archivos = carpeta?.listFiles { f -> f.name.endsWith(".json") } ?: return
        archivos.forEach { archivo ->
            val fallo = runCatching { JSONObject(archivo.readText()) }.getOrNull()
            if (fallo != null) {
                enviar(uid, "app_crash", mapOf(
                    "sessionId" to fallo.optString("sessionId"),
                    "screen" to fallo.optString("screen"),
                    "error" to fallo.optString("error"),
                    "appVersion" to fallo.optString("appVersion"),
                    "crashedAt" to fallo.optLong("crashedAt")
                ))
            }
            archivo.delete()
        }
        if (archivos.isNotEmpty()) Log.d(TAG, "Fallos subidos: ${archivos.size}")
    }

    private fun enviar(uid: String, nombre: String, datos: Map<String, Any>) {
        val evento = datos + mapOf(
            "name" to nombre,
            "teacherId" to uid,
            "platform" to InfoDispositivo.PLATAFORMA,
            "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "androidVersion" to Build.VERSION.SDK_INT.toString(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        // Firestore lo guarda en el celular y lo sube cuando haya red
        runCatching { FirebaseModule.firestore.collection("events").add(evento) }
    }
}
