package com.enad.enadmovil.feature.ayuda

import com.enad.enadmovil.data.remote.FirebaseModule
import com.google.firebase.firestore.FieldValue

object GuiaEventos {
    const val INICIADA = "tutorial_started"
    const val PASO_COMPLETADO = "tutorial_step_completed"
    const val PASO_OMITIDO = "tutorial_step_skipped"
    const val COMPLETADA = "tutorial_completed"
    const val ABANDONADA = "tutorial_abandoned"

    private fun idDe(guia: Guia) = when (guia) {
        Guia.HORAS -> "hours"
        Guia.ASISTENCIA -> "attendance"
        Guia.EVALUACION -> "evaluation"
        Guia.ACTIVIDADES -> "activities"
    }

    fun registrar(nombre: String, guia: Guia, corrida: String, paso: Int) {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return
        runCatching {
            FirebaseModule.firestore.collection("events").add(
                mapOf(
                    "name" to nombre,
                    "tutorial_id" to idDe(guia),
                    "tutorial_version" to 2,
                    "run_id" to corrida,
                    "step" to paso,
                    "teacherId" to uid,
                    "platform" to "kotlin",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
    }
}