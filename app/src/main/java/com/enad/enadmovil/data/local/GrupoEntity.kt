package com.enad.enadmovil.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.enad.enadmovil.domain.model.AreaMateria

@Entity(tableName = "grupos")
data class GrupoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val docente: String,
    val area: AreaMateria
)
