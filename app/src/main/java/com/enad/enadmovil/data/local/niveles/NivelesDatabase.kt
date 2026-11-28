package com.enad.enadmovil.data.local.niveles

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base SQLite propia para los niveles por institución (enad_niveles_v1.db), aparte de AppDatabase
 * para no migrarla: es privada de la app, persiste y separa los datos por docente (uid).
 */
@Database(entities = [ConteoNivelEntity::class], version = 1, exportSchema = false)
abstract class NivelesDatabase : RoomDatabase() {
    abstract fun nivelesDao(): NivelesDao

    companion object {
        @Volatile private var instancia: NivelesDatabase? = null

        /** Una sola instancia para toda la app. */
        fun obtener(context: Context): NivelesDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    NivelesDatabase::class.java,
                    "enad_niveles_v1.db"
                ).build().also { instancia = it }
            }
        }
    }
}
