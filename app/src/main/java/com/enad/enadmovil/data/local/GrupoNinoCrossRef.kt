package com.enad.enadmovil.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "grupo_nino_cross_ref",
    primaryKeys = ["grupoId", "ninoId"],
    indices = [Index("ninoId")]
)
data class GrupoNinoCrossRef(
    val grupoId: Int,
    val ninoId: Int
)
