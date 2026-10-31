package com.enad.enadmovil.data.telemetria

import com.enad.enadmovil.data.remote.FirebaseModule
import com.enad.enadmovil.domain.model.RiesgoAbandono
import com.google.firebase.firestore.FieldValue

object EventosReenganche {
    const val MOSTRADA = "reengagement_card_shown"
    const val USADA = "reengagement_card_used"

    fun registrar(nombre: String, riesgo: RiesgoAbandono) {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return
        val evento = mapOf(
            "name" to nombre,
            "teacherId" to uid,
            "platform" to InfoDispositivo.PLATAFORMA,
            "lastWeekOpens" to riesgo.semanaPasada,
            "usualWeekOpens" to riesgo.semanaHabitual,
            "createdAt" to FieldValue.serverTimestamp()
        )
        runCatching { FirebaseModule.firestore.collection("events").add(evento) }
    }
}