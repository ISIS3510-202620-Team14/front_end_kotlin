package com.enad.enadmovil.data.telemetria

import com.enad.enadmovil.data.remote.FirebaseModule
import com.enad.enadmovil.domain.model.CondicionesCaptura
import com.enad.enadmovil.domain.model.ResumenRevision
import com.google.firebase.firestore.FieldValue

/**
 * Por cada página escaneada se guarda en "events" con qué luz y movimiento se tomó la foto y
 * cuántas filas leyó el OCR. Con eso se responde si la ayuda de los sensores mejora la lectura:
 * filas leídas y fallos por nivel de luz, quietud, linterna y disparo automático.
 */
object EventosEscaneo {

    /** [filas] null = el OCR falló con esa página. */
    fun registrar(uid: String, condiciones: CondicionesCaptura, filas: Int?, duracionMs: Long) {
        val evento = mapOf(
            "name" to "scan_page",
            "teacherId" to uid,
            "platform" to InfoDispositivo.PLATAFORMA,
            "lux" to condiciones.lux,
            "lightLevel" to condiciones.nivelLuz.clave,
            "steady" to condiciones.estable,
            "torch" to condiciones.linterna,
            "autoCapture" to condiciones.automatica,
            "rows" to (filas ?: 0),
            "ocrFailed" to (filas == null),
            "ocrMs" to duracionMs,
            "createdAt" to FieldValue.serverTimestamp()
        )
        // Firestore lo guarda en el celular y lo sube cuando haya red; si falla solo se pierde el dato
        runCatching { FirebaseModule.firestore.collection("events").add(evento) }
    }

    /**
     * BQ #11: al importar una lista escaneada, cuántas filas dejó el docente como las leyó el OCR,
     * cuántas corrigió (y en qué campo) y cuántas borró. Solo conteos, nunca nombres ni códigos.
     * [origen]: attendance (Asistencia) o manage_students (Gestionar estudiantes).
     */
    fun registrarRevision(uid: String, resumen: ResumenRevision, origen: String) {
        val evento = mapOf(
            "name" to "scan_review",
            "teacherId" to uid,
            "platform" to InfoDispositivo.PLATAFORMA,
            "source" to origen,
            "rowsRead" to resumen.leidas,
            "rowsUnchanged" to resumen.sinCambios,
            "rowsEdited" to resumen.editadas,
            "namesEdited" to resumen.nombresEditados,
            "codesEdited" to resumen.codigosEditados,
            "rowsDeleted" to resumen.borradas,
            "createdAt" to FieldValue.serverTimestamp()
        )
        runCatching { FirebaseModule.firestore.collection("events").add(evento) }
    }
}
