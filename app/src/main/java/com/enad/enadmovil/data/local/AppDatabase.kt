package com.enad.enadmovil.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.enad.enadmovil.data.local.entity.AsistenciaEntity
import com.enad.enadmovil.data.local.entity.EstudianteEntity

@Database(
    entities = [EstudianteEntity::class, AsistenciaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // Los DAO se agregan aquí en el siguiente paso.

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "enad.db"
                ).build().also { instancia = it }
            }
    }
}