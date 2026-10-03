package com.enad.enadmovil.domain.model

enum class ActivitySource(val storageValue: String) {
    LIBRARY("library"),
    CUSTOM("custom")
}

data class Actividad(
    val id: String,
    val title: String,
    val subject: String,
    val minutes: Int,
    val source: ActivitySource,
    val templateId: String? = null
)

data class ActivityTemplate(
    val id: String,
    val title: String,
    val subject: String,
    val minutes: Int,
    val description: String
)