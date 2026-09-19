package com.enad.enadmovil.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ninos")
data class NinoEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val nivel: String,
    val grado: Int
)
