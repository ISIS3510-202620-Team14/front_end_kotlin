package com.enad.enadmovil.ui.screens.admin

import com.enad.enadmovil.feature.ayuda.Guia
import com.enad.enadmovil.feature.ayuda.PASOS_GUIA

/** Con menos corridas que esto el reporte no saca conclusión de una guía. */
internal const val MINIMO_CORRIDAS_GUIA = 5

/** El id que mandan las dos apps en los eventos tutorial_*. */
internal fun guiaDeId(id: String): Guia? = when (id) {
    "hours" -> Guia.HORAS
    "attendance" -> Guia.ASISTENCIA
    "evaluation" -> Guia.EVALUACION
    "activities" -> Guia.ACTIVIDADES
    else -> null
}

/** El paso llega desde 0. Se muestra desde 1 y con su título cuando existe en la guía. */
internal fun nombrePaso(guia: Guia?, paso: Int): String {
    val titulo = guia?.let { PASOS_GUIA[it]?.getOrNull(paso)?.titulo }
    return if (titulo != null) "Paso ${paso + 1}: $titulo" else "Paso ${paso + 1}"
}
