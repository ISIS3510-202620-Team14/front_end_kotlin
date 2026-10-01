package com.enad.enadmovil.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Nombres alineados con el backend (fullName, code, grade, age, provisional). */
@Entity(tableName = "estudiantes")
data class EstudianteEntity(
    @PrimaryKey val id: String,       // = clientId en el backend, así un reintento no duplica
    val schoolId: String,
    val docenteUid: String,           // quién lo creó: solo se sube con la sesión de ese docente
    val fullName: String,
    val code: String?,                // null = provisional: el servidor asigna PROV-XXXX
    val grade: Int?,                  // 3, 4 o 5
    val age: Int?,
    val provisional: Boolean,
    val syncStatus: SyncStatus
)