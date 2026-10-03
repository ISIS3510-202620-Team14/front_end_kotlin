package com.enad.enadmovil.feature.actividades

import com.enad.enadmovil.data.remote.FirebaseModule
import com.enad.enadmovil.domain.model.Actividad
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

/**
 * BQ 4:
 * What percentage of teachers use the activity library versus creating their own activities?
 *
 * Mantiene la misma medición de Flutter:
 * - evento: activity_selected
 * - source: library/custom
 * - teacherId: docente autenticado
 */
class ActividadesRepository {

    private val events = FirebaseModule.firestore.collection("events")

    suspend fun cargarUso(): ActivityUsage {
        val uid = FirebaseModule.auth.currentUser?.uid
            ?: return ActivityUsage()

        val snapshot = events
            .whereEqualTo("teacherId", uid)
            .whereEqualTo("name", "activity_selected")
            .get()
            .await()

        var libraryCount = 0
        var customCount = 0

        snapshot.documents.forEach { document ->
            when (document.getString("source")) {
                "library" -> libraryCount++
                "custom" -> customCount++
            }
        }

        return ActivityUsage(
            libraryCount = libraryCount,
            customCount = customCount
        )
    }

    suspend fun cargarActividadesPlaneadas(): List<Actividad> {
        val uid = FirebaseModule.auth.currentUser?.uid
            ?: return emptyList()

        val snapshot = events
            .whereEqualTo("teacherId", uid)
            .whereEqualTo("name", "activity_selected")
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            val source = when (document.getString("source")) {
                "library" -> com.enad.enadmovil.domain.model.ActivitySource.LIBRARY
                "custom" -> com.enad.enadmovil.domain.model.ActivitySource.CUSTOM
                else -> return@mapNotNull null
            }

            val id = document.getString("activityId")
                ?: return@mapNotNull null

            val title = document.getString("title")
                ?: return@mapNotNull null

            val subject = document.getString("subject")
                ?: return@mapNotNull null

            val minutes = document.getLong("minutes")?.toInt()
                ?: return@mapNotNull null

            Actividad(
                id = id,
                title = title,
                subject = subject,
                minutes = minutes,
                source = source,
                templateId = document.getString("templateId")
            )
        }
    }
    suspend fun registrarActividadSeleccionada(activity: Actividad) {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return

        val event = mutableMapOf<String, Any>(
            "name" to "activity_selected",
            "activityId" to activity.id,
            "title" to activity.title,
            "subject" to activity.subject,
            "minutes" to activity.minutes,
            "source" to activity.source.storageValue,
            "teacherId" to uid,
            "platform" to "kotlin",
            "createdAt" to FieldValue.serverTimestamp()
        )

        activity.templateId?.let {
            event["templateId"] = it
        }

        events.add(event).await()
    }
}

data class ActivityUsage(
    val libraryCount: Int = 0,
    val customCount: Int = 0
) {
    val total: Int
        get() = libraryCount + customCount

    val libraryPercent: Int
        get() = if (total == 0) 0 else (libraryCount * 100 / total)

    val customPercent: Int
        get() = if (total == 0) 0 else 100 - libraryPercent
}