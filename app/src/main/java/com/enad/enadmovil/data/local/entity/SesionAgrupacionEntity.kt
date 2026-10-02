package com.enad.enadmovil.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sesiones_agrupacion")
data class SesionAgrupacionEntity(@PrimaryKey val id: String, val docenteUid: String, val schoolId: String?,
                                  val subject: String, val groupName: String, val studentsCounted: Int,
                                  val teachersCounted: Int, val studentsAssigned: Int, val creadaEn: Long,
                                  val connectivity: String, val platform: String, val appVersion: String,
                                  val syncStatus: SyncStatus, val intentos: Int = 0, val sincronizadaEn: Long? = null,
                                  val ultimoError: String? = null)