package com.enad.enadmovil.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class GrupoConNinos(
    @Embedded val grupo: GrupoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = GrupoNinoCrossRef::class,
            parentColumn = "grupoId",
            entityColumn = "ninoId"
        )
    )
    val ninos: List<NinoEntity>
)
