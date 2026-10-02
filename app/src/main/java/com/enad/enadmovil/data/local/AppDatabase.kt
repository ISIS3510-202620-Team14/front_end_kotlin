package com.enad.enadmovil.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.enad.enadmovil.data.local.dao.AperturaDao
import com.enad.enadmovil.data.local.dao.AsistenciaDao
import com.enad.enadmovil.data.local.dao.EstudianteDao
import com.enad.enadmovil.data.local.dao.SesionAgrupacionDao
import com.enad.enadmovil.data.local.entity.AperturaEntity
import com.enad.enadmovil.data.local.entity.AsistenciaEntity
import com.enad.enadmovil.data.local.entity.EstudianteEntity
import com.enad.enadmovil.data.local.entity.SesionAgrupacionEntity

@Database(
    entities = [
        EstudianteEntity::class,
        AsistenciaEntity::class,
        SesionAgrupacionEntity::class,
        AperturaEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun estudianteDao(): EstudianteDao
    abstract fun asistenciaDao(): AsistenciaDao
    abstract fun sesionAgrupacionDao(): SesionAgrupacionDao
    abstract fun aperturaDao(): AperturaDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        // Adds the sesiones_agrupacion table
        private val MIGRACION_1_2 = object : Migration(1, 2) {
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

        // Adds the aperturas table without touching existing data
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `aperturas` (" +
                            "`id` TEXT NOT NULL, `docenteUid` TEXT NOT NULL, `abiertaEn` INTEGER NOT NULL, " +
                            "`semana` TEXT NOT NULL, `platform` TEXT NOT NULL, `appVersion` TEXT NOT NULL, " +
                            "`syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_aperturas_docenteUid_semana` " +
                            "ON `aperturas` (`docenteUid`, `semana`)"
                )
            }
        }

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "enad.db"
                ).addMigrations(MIGRACION_1_2, MIGRACION_2_3).build().also { instancia = it }
            }
    }
}