package com.enad.enadmovil.data.local

import androidx.room.TypeConverter
import com.enad.enadmovil.domain.model.AreaMateria

class Converters {
    @TypeConverter
    fun fromAreaMateria(area: AreaMateria): String = area.name

    @TypeConverter
    fun toAreaMateria(valor: String): AreaMateria = AreaMateria.valueOf(valor)
}
