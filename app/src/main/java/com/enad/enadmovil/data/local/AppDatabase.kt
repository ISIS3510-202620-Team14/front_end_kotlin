package com.enad.enadmovil.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.enad.enadmovil.data.local.dao.AperturaDao
import com.enad.enadmovil.data.local.dao.AsistenciaDao
import com.enad.enadmovil.data.local.dao.ContextoDao
import com.enad.enadmovil.data.local.dao.EstudianteDao
import com.enad.enadmovil.data.local.dao.SesionAgrupacionDao
import com.enad.enadmovil.data.local.entity.AperturaEntity
import com.enad.enadmovil.data.local.entity.AsistenciaEntity
import com.enad.enadmovil.data.local.entity.ContextoDiaEntity
import com.enad.enadmovil.data.local.entity.EstudianteEntity
import com.enad.enadmovil.data.local.entity.GrupoHorarioEntity
import com.enad.enadmovil.data.local.entity.PresenciaSedeEntity
import com.enad.enadmovil.data.local.entity.ReporteHorasEntity
import com.enad.enadmovil.data.local.entity.SedeEntity
import com.enad.enadmovil.data.local.entity.SesionAgrupacionEntity

@Database(
    entities = [
        EstudianteEntity::class,
        AsistenciaEntity::class,
        SesionAgrupacionEntity::class,
        AperturaEntity::class,
        SedeEntity::class,
        GrupoHorarioEntity::class,
        PresenciaSedeEntity::class,
        ContextoDiaEntity::class,
        ReporteHorasEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun estudianteDao(): EstudianteDao
    abstract fun asistenciaDao(): AsistenciaDao
    abstract fun sesionAgrupacionDao(): SesionAgrupacionDao
    abstract fun aperturaDao(): AperturaDao
    abstract fun contextoDao(): ContextoDao

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
        // Adds the context-aware tables (sedes, grupos con horario, presencia, contexto del día, horas)
        private val MIGRACION_3_4 = object : Migration(3,4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sedes` (`id` TEXT NOT NULL, `schoolId` TEXT NOT NULL, " +
                    "`campusId` TEXT NOT NULL, `nombre` TEXT NOT NULL, `escuela` TEXT NOT NULL, " +
                    "`lat` REAL, `lng` REAL, PRIMARY KEY(`id`))")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `grupos_horario` (`id` TEXT NOT NULL, `docenteUid` TEXT NOT NULL, " +
                            "`nombre` TEXT NOT NULL, `materia` TEXT NOT NULL, `schoolId` TEXT NOT NULL, " +
                            "`campusId` TEXT, `horario` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `presencia_sede` (`docenteUid` TEXT NOT NULL, `fecha` TEXT NOT NULL, " +
                            "`sedeId` TEXT NOT NULL, `primeraVez` INTEGER NOT NULL, `ultimaVez` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`docenteUid`, `fecha`, `sedeId`))")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `contexto_dia` (`docenteUid` TEXT NOT NULL, `fecha` TEXT NOT NULL, " +
                            "`grupoId` TEXT NOT NULL, `origen` TEXT NOT NULL, `confirmadoEn` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`docenteUid`, `fecha`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reportes_horas` (`docenteUid` TEXT NOT NULL, `fecha` TEXT NOT NULL, " +
                            "`horasPlaneadas` REAL NOT NULL, `horasRealizadas` REAL NOT NULL, `origen` TEXT NOT NULL, " +
                            "`motivo` TEXT, `presenteEnSede` INTEGER NOT NULL, `guardadoEn` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`docenteUid`, `fecha`))"
                )

            }
        }

        // Adds the sync state of the worked-hours reports; existing reports are uploaded on the next sync
        private val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `reportes_horas` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE `reportes_horas` ADD COLUMN `intentos` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `reportes_horas` ADD COLUMN `ultimoError` TEXT")
            }
        }

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "enad.db"
                ).addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5).build().also { instancia = it }
            }
    }
}