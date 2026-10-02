package com.enad.enadmovil.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.enad.enadmovil.data.local.dao.AsistenciaDao
import com.enad.enadmovil.data.local.dao.EstudianteDao
import com.enad.enadmovil.data.local.dao.SesionAgrupacionDao
import com.enad.enadmovil.data.local.entity.AsistenciaEntity
import com.enad.enadmovil.data.local.entity.EstudianteEntity
import com.enad.enadmovil.data.local.entity.SesionAgrupacionEntity

@Database(
    entities = [EstudianteEntity::class, AsistenciaEntity::class, SesionAgrupacionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun estudianteDao(): EstudianteDao
    abstract fun asistenciaDao(): AsistenciaDao
    abstract fun sesionAgrupacionDao(): SesionAgrupacionDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null
        private val MIGRACION_1_2 = object : Migration(1,2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sesiones_agrupacion` (" +
                    "`id` TEXT NOT NULL, `docenteUid` TEXT NOT NULL, `schoolId` TEXT, " +
                    "`subject` TEXT NOT NULL, `groupName` TEXT NOT NULL, " +
                    "`studentsCounted` INTEGER NOT NULL, `teachersCounted` INTEGER NOT NULL, " +
                    "`studentsAssigned` INTEGER NOT NULL, `creadaEn` INTEGER NOT NULL, " +
                    "`connectivity` TEXT NOT NULL, `platform` TEXT NOT NULL, `appVersion` TEXT NOT NULL, " +
                    "`syncStatus` TEXT NOT NULL, `intentos` INTEGER NOT NULL, " +
                    "`sincronizadaEn` INTEGER, `ultimoError` TEXT, PRIMARY KEY(`id`))"
                )
            }
        }
        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "enad.db"
                ).addMigrations(MIGRACION_1_2).build().also { instancia = it }
            }
    }
}