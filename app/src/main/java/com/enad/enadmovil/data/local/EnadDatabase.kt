package com.enad.enadmovil.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [GrupoEntity::class, NinoEntity::class, GrupoNinoCrossRef::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EnadDatabase : RoomDatabase() {
    abstract fun gruposDao(): GruposDao

    companion object {
        @Volatile
        private var instancia: EnadDatabase? = null

        fun obtener(context: Context): EnadDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    EnadDatabase::class.java,
                    "enad_movil.db"
                ).build().also { instancia = it }
            }
        }
    }
}
