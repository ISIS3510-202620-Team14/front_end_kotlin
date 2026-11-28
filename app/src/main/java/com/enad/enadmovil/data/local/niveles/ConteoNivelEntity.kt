package com.enad.enadmovil.data.local.niveles

import androidx.room.Entity
import androidx.room.Index

/**
 * Una celda de los conteos de una institución: cuántos estudiantes de un grado están en un nivel
 * de una materia. Solo se guardan las celdas con estudiantes.
 */
@Entity(
    tableName = "conteos_nivel",
    primaryKeys = ["docenteUid", "schoolId", "materia", "grado", "nivel"],
    indices = [Index(value = ["docenteUid", "materia", "grado"])] // los filtros de la vista
)
data class ConteoNivelEntity(
    val docenteUid: String,
    val schoolId: String,
    val nombreInstitucion: String,
    val materia: String,      // lectura | matematicas
    val grado: Int,           // 3, 4 o 5
    val nivel: Int,           // 0 = sin evaluar, 1..6 = nivel ENAd
    val estudiantes: Int,
    val guardadoEn: Long      // epoch millis
)

/** Resultado del GROUP BY: estudiantes de una institución en un nivel, sumando los grados elegidos. */
data class SumaNivel(
    val schoolId: String,
    val nombreInstitucion: String,
    val nivel: Int,
    val estudiantes: Int,
    val guardadoEn: Long
)
